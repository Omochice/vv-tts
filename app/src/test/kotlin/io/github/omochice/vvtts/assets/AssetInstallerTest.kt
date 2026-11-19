package io.github.omochice.vvtts.assets

import com.sun.net.httpserver.HttpExchange
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class AssetInstallerTest {
  @get:Rule val tmp = TemporaryFolder()

  private val ortTop = "voicevox_onnxruntime-android-arm64-1.23.2"
  private val ortArchive =
      TarFixture()
          .directory("$ortTop/")
          .directory("$ortTop/lib/")
          .file("$ortTop/lib/libvoicevox_onnxruntime.so", "the library")
          .file("$ortTop/VERSION", "1.23.2")
          .gzip()
  private val dictArchive =
      TarFixture()
          .directory("open_jtalk_dic_utf_8-1.11/")
          .file("open_jtalk_dic_utf_8-1.11/sys.dic", "the dictionary")
          .file("open_jtalk_dic_utf_8-1.11/matrix.bin", "the matrix")
          .gzip()
  private val voiceModel = Random(7).nextBytes(300_000)

  private val ortServer = TestHttpServer(TestHttpServer.ranged(ortArchive))
  private val dictServer = TestHttpServer(TestHttpServer.ranged(dictArchive))
  private val voiceServer = TestHttpServer(TestHttpServer.ranged(voiceModel))

  @AfterTest
  fun stopServers() {
    ortServer.close()
    dictServer.close()
    voiceServer.close()
  }

  private val ortPin = pin("ort.tgz", ortArchive, ortServer)
  private val dictPin = pin("dict.tar.gz", dictArchive, dictServer)
  private val voicePin = pin("0.vvm", voiceModel, voiceServer)

  private fun pin(fileName: String, bytes: ByteArray, server: TestHttpServer) =
      RemoteFile(
          fileName = fileName,
          url = server.url,
          sizeBytes = bytes.size.toLong(),
          sha256 = sha256Hex(bytes),
      )

  private fun catalog(dictionary: RemoteFile = dictPin, voice: RemoteFile = voicePin) =
      AssetCatalog(
          dictionaryArchive = dictionary,
          terms = emptyList(),
          voiceModels = listOf(VoiceModel(voice, emptyList())),
          onnxRuntimeArchives = mapOf("arm64-v8a" to ortPin),
      )

  private val layout
    get() = AssetLayout(tmp.root)

  private fun installer(abi: String = "arm64-v8a", catalog: AssetCatalog = catalog()) =
      AssetInstaller(catalog, layout, abi)

  private fun AssetInstaller.install(
      onProgress: (Long, Long) -> Unit = { _, _ -> },
      isStopRequested: () -> Boolean = { false },
  ) = install("0.vvm", onProgress, isStopRequested)

  @Test
  fun `a fresh install makes the voice model speakable`() {
    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertTrue(layout.isInstalled("0.vvm"))
    assertEquals("the library", layout.onnxRuntimeLibrary.readText())
    assertEquals("the dictionary", File(layout.dictionaryDir, "sys.dic").readText())
    assertContentEquals(voiceModel, layout.voiceModel("0.vvm").readBytes())
  }

  @Test
  fun `every installed file is read-only`() {
    installer().install()

    val files = tmp.root.walk().filter { it.isFile }.toList()
    assertEquals(5, files.size)
    assertEquals(emptyList(), files.filter { it.canWrite() })
  }

  @Test
  fun `a fresh install leaves neither archives nor temporary directories behind`() {
    installer().install()

    assertEquals(setOf("ort", "dict", "vvm", "downloads"), tmp.root.list()!!.toSet())
    assertEquals(emptyList(), layout.downloadsDir.list()!!.toList())
  }

  @Test
  fun `parts copied in by hand are not downloaded again`() {
    layout.installBasePartsByHand()

    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertEquals(emptyList(), ortServer.ranges)
    assertEquals(emptyList(), dictServer.ranges)
    assertEquals(1, voiceServer.ranges.size)
    assertEquals("copied by hand", layout.onnxRuntimeLibrary.readText())
  }

  @Test
  fun `a second install of the same voice model contacts no server`() {
    installer().install()

    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertEquals(1, ortServer.ranges.size)
    assertEquals(1, dictServer.ranges.size)
    assertEquals(1, voiceServer.ranges.size)
  }

  @Test
  fun `an archive left downloaded by an interrupted run is unpacked without downloading it again`() {
    File(layout.downloadsDir, ortPin.fileName).create().writeBytes(ortArchive)

    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertEquals(emptyList(), ortServer.ranges)
    assertEquals("the library", layout.onnxRuntimeLibrary.readText())
  }

  @Test
  fun `temporary directories left by an interrupted extraction do not break the next run`() {
    File(tmp.root, "ort.tmp/lib/libvoicevox_onnxruntime.so").create().setReadOnly()
    File(tmp.root, "dict.tmp/sys.dic").create().setReadOnly()

    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertEquals("the library", layout.onnxRuntimeLibrary.readText())
    assertEquals("the dictionary", File(layout.dictionaryDir, "sys.dic").readText())
    assertFalse(File(tmp.root, "ort.tmp").exists())
    assertFalse(File(tmp.root, "dict.tmp").exists())
  }

  @Test
  fun `incomplete parts in place are replaced`() {
    layout.dictionaryDir.mkdirs()
    File(layout.onnxRuntimeDir, "lib/unrelated.so").create()

    val result = installer().install()

    assertEquals(InstallResult.Installed, result)
    assertTrue(layout.isInstalled("0.vvm"))
    assertFalse(File(layout.onnxRuntimeDir, "lib/unrelated.so").exists())
  }

  @Test
  fun `a stopped install keeps its partial download and a later call completes it`() {
    val partial = File(layout.voiceModel("0.vvm").path + ".part")

    val stopped = installer().install { partial.length() >= 100_000 }

    assertEquals(InstallResult.Stopped, stopped)
    assertFalse(layout.isInstalled("0.vvm"))
    val kept = partial.length()

    val resumed = installer().install()

    assertEquals(InstallResult.Installed, resumed)
    assertTrue(layout.isInstalled("0.vvm"))
    assertEquals("bytes=$kept-", voiceServer.ranges.last())
    assertEquals(1, ortServer.ranges.size)
    assertEquals(1, dictServer.ranges.size)
  }

  @Test
  fun `a stop before any read installs nothing`() {
    val result = installer().install { true }

    assertEquals(InstallResult.Stopped, result)
    assertFalse(layout.onnxRuntimeDir.exists())
    assertEquals(emptyList(), dictServer.ranges)
  }

  @Test
  fun `an archive not matching its pin is a download failure and installs nothing for it`() {
    dictServer.handler = TestHttpServer.full(dictArchive.reversedArray())

    val result = installer().install()

    assertEquals(InstallResult.DownloadFailed(dictPin, DownloadResult.Corrupt), result)
    assertFalse(layout.dictionaryDir.exists())
    assertFalse(File(tmp.root, "dict.tmp").exists())
    assertEquals(emptyList(), voiceServer.ranges)
  }

  @Test
  fun `an error status is reported with the file it came from`() {
    voiceServer.handler = { it.sendResponseHeaders(503, -1) }

    val result = installer().install()

    assertEquals(
        InstallResult.DownloadFailed(voicePin, DownloadResult.UnexpectedStatus(503)),
        result,
    )
    assertFalse(layout.voiceModel("0.vvm").exists())
  }

  @Test
  fun `an archive matching its pin but not extractable fails, installs nothing and is discarded`() {
    val broken = "not a gzip stream".toByteArray()
    dictServer.handler = TestHttpServer.ranged(broken)
    val brokenPin = pin("dict.tar.gz", broken, dictServer)

    val result = installer(catalog = catalog(dictionary = brokenPin)).install()

    val failure = assertIs<InstallResult.PlacementFailed>(result)
    assertEquals(brokenPin, failure.file)
    assertIs<MalformedArchiveException>(failure.cause)
    assertFalse(layout.dictionaryDir.exists())
    assertFalse(File(tmp.root, "dict.tmp").exists())
    assertEquals(emptyList(), layout.downloadsDir.list()!!.toList())
  }

  @Test
  fun `an ABI without an ONNX Runtime build fails before downloading anything`() {
    val result = installer(abi = "mips").install()

    assertEquals(InstallResult.NoOnnxRuntimeFor("mips"), result)
    assertEquals(emptyList(), ortServer.ranges)
    assertEquals(emptyList(), dictServer.ranges)
    assertEquals(emptyList(), voiceServer.ranges)
  }

  @Test
  fun `an ABI without an ONNX Runtime build is fine when the library is already present`() {
    layout.installOnnxRuntimeByHand()

    val result = installer(abi = "mips").install()

    assertEquals(InstallResult.Installed, result)
  }

  @Test
  fun `a voice model the catalog does not know is rejected`() {
    assertFailsWith<IllegalArgumentException> {
      installer().install("99.vvm", { _, _ -> }, { false })
    }
  }

  @Test
  fun `progress counts up across the files still to download to their total size`() {
    layout.installDictionaryByHand()
    val progress = mutableListOf<Pair<Long, Long>>()

    installer().install(onProgress = { done, total -> progress += done to total })

    val total = ortPin.sizeBytes + voicePin.sizeBytes
    assertTrue(
        progress.all { (_, t) -> t == total },
        "totals ${progress.map { it.second }.toSet()}",
    )
    assertEquals(progress.map { it.first }.sorted(), progress.map { it.first })
    assertEquals(total to total, progress.last())
  }

  @Test
  fun `a fresh root has every part to download`() {
    assertEquals(
        ortPin.sizeBytes + dictPin.sizeBytes + voicePin.sizeBytes,
        installer().bytesToDownload("0.vvm"),
    )
  }

  @Test
  fun `parts in place and archives already downloaded count for nothing`() {
    layout.installOnnxRuntimeByHand()
    File(layout.downloadsDir, dictPin.fileName).create().writeBytes(dictArchive)

    assertEquals(voicePin.sizeBytes, installer().bytesToDownload("0.vvm"))
  }

  @Test
  fun `an installed voice model has nothing to download`() {
    installer().install()

    assertEquals(0, installer().bytesToDownload("0.vvm"))
  }

  @Test
  fun `the bytes to download are the total that progress reports`() {
    layout.installDictionaryByHand()
    val expected = installer().bytesToDownload("0.vvm")
    val totals = mutableSetOf<Long>()

    installer().install(onProgress = { _, total -> totals += total })

    assertEquals(setOf(expected), totals)
  }

  @Test
  fun `an ABI without an ONNX Runtime build leaves the library out of the bytes to download`() {
    assertEquals(
        dictPin.sizeBytes + voicePin.sizeBytes,
        installer(abi = "mips").bytesToDownload("0.vvm"),
    )
  }

  @Test
  fun `the bytes to download of a voice model the catalog does not know are rejected`() {
    assertFailsWith<IllegalArgumentException> { installer().bytesToDownload("99.vvm") }
  }

  @Test
  fun `concurrent installs into different roots never download at the same time`() {
    val inFlight = AtomicInteger()
    val maxInFlight = AtomicInteger()
    val countingHandler = { exchange: HttpExchange ->
      maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
      Thread.sleep(300)
      // Counted down before answering rather than after, because once the body is sent the other
      // install may already have started its request.
      inFlight.decrementAndGet()
      TestHttpServer.ranged(voiceModel)(exchange)
    }
    // One server per install, because a test server answers one request at a time and would
    // serialize the downloads by itself.
    val servers = List(2) { TestHttpServer(countingHandler) }
    try {
      val installers = servers.mapIndexed { index, server ->
        val rootLayout = AssetLayout(tmp.newFolder("root$index"))
        rootLayout.installBasePartsByHand()
        val catalog = catalog(voice = pin("0.vvm", voiceModel, server))
        AssetInstaller(catalog, rootLayout, "arm64-v8a")
      }
      val results = arrayOfNulls<InstallResult>(installers.size)

      installers
          .mapIndexed { index, installer ->
            thread { results[index] = installer.install("0.vvm", { _, _ -> }, { false }) }
          }
          .forEach { it.join() }

      assertEquals(listOf(InstallResult.Installed, InstallResult.Installed), results.toList())
      assertEquals(1, maxInFlight.get())
    } finally {
      servers.forEach { it.close() }
    }
  }
}
