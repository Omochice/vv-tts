package io.github.omochice.vvtts.assets

import java.io.File
import java.net.SocketTimeoutException
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class AssetDownloaderTest {
  @get:Rule val tmp = TemporaryFolder()

  private val content = Random(42).nextBytes(300_000)
  private val server = TestHttpServer(TestHttpServer.ranged(content))
  private val pin = pinFor(content, server.url)

  @AfterTest fun stopServer() = server.close()

  private fun pinFor(bytes: ByteArray, url: String) =
      RemoteFile(
          fileName = "asset",
          url = url,
          sizeBytes = bytes.size.toLong(),
          sha256 =
              MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
              },
      )

  private fun target() = File(tmp.root, "asset")

  private fun partial() = File(tmp.root, "asset.part")

  @Test
  fun `a fresh download installs the pinned content at the target`() {
    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `the installed file is read-only and leaves no partial file behind`() {
    AssetDownloader().download(pin, target()) { false }

    assertFalse(target().canWrite())
    assertEquals(listOf("asset"), tmp.root.list()!!.toList())
  }

  @Test
  fun `content that does not match the pin is deleted and nothing is installed`() {
    server.handler = TestHttpServer.full(content.reversedArray())

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Corrupt, result)
    assertEquals(emptyList(), tmp.root.list()!!.toList())
  }

  @Test
  fun `an existing target counts as installed without contacting the server`() {
    target().writeText("installed earlier")

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertEquals(emptyList(), server.ranges)
    assertEquals("installed earlier", target().readText())
  }

  @Test
  fun `an existing partial file is resumed with a range request for the rest`() {
    partial().writeBytes(content.copyOf(100_000))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertEquals(listOf<String?>("bytes=100000-"), server.ranges)
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `a server ignoring the range request makes the download start over`() {
    server.handler = TestHttpServer.full(content)
    partial().writeBytes(content.copyOf(100_000))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertEquals(listOf<String?>("bytes=100000-"), server.ranges)
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `a partial file already holding every byte is installed without contacting the server`() {
    partial().writeBytes(content)

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertEquals(emptyList(), server.ranges)
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `a range rejected as unsatisfiable deletes the short partial file`() {
    server.handler = { it.sendResponseHeaders(416, -1) }
    partial().writeBytes(content.copyOf(100_000))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Corrupt, result)
    assertEquals(emptyList(), tmp.root.list()!!.toList())
  }

  @Test
  fun `a partial file longer than the pin is deleted without contacting the server`() {
    partial().writeBytes(content + ByteArray(10))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Corrupt, result)
    assertEquals(emptyList(), server.ranges)
    assertEquals(emptyList(), tmp.root.list()!!.toList())
  }

  @Test
  fun `a response cut off mid-stream keeps the received bytes for the next attempt to resume`() {
    server.handler = TestHttpServer.aborted(content, sentBytes = 150_000)

    val result = AssetDownloader().download(pin, target()) { false }

    assertIs<DownloadResult.TransferFailed>(result)
    assertContentEquals(content.copyOf(150_000), partial().readBytes())
    assertFalse(target().exists())

    server.handler = TestHttpServer.ranged(content)
    val retry = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, retry)
    assertEquals("bytes=150000-", server.ranges.last())
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `bytes beyond the pinned size are not written`() {
    server.handler = TestHttpServer.full(content + ByteArray(50_000))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, result)
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `a stop request keeps the bytes received so far for a later resume`() {
    val result = AssetDownloader().download(pin, target()) { partial().length() >= 100_000 }

    assertEquals(DownloadResult.Stopped, result)
    val kept = partial().readBytes()
    assertTrue(kept.size in 100_000 until content.size, "kept ${kept.size} bytes")
    assertContentEquals(content.copyOf(kept.size), kept)
    assertFalse(target().exists())

    val resumed = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.Installed, resumed)
    assertEquals("bytes=${kept.size}-", server.ranges.last())
    assertContentEquals(content, target().readBytes())
  }

  @Test
  fun `progress counts bytes held so far against the pinned total, including resumed bytes`() {
    partial().writeBytes(content.copyOf(100_000))
    val progress = mutableListOf<Pair<Long, Long>>()

    AssetDownloader().download(pin, target(), { done, total -> progress += done to total }) {
      false
    }

    assertTrue(progress.isNotEmpty())
    assertTrue(progress.all { (_, total) -> total == content.size.toLong() })
    assertTrue(progress.first().first > 100_000)
    assertEquals(progress.map { it.first }.sorted(), progress.map { it.first })
    assertEquals(content.size.toLong(), progress.last().first)
  }

  @Test
  fun `an error status is reported with its code and keeps the partial file`() {
    server.handler = { it.sendResponseHeaders(503, -1) }
    partial().writeBytes(content.copyOf(100_000))

    val result = AssetDownloader().download(pin, target()) { false }

    assertEquals(DownloadResult.UnexpectedStatus(503), result)
    assertContentEquals(content.copyOf(100_000), partial().readBytes())
  }

  @Test
  fun `a resumed download follows a redirect to another origin and still asks for the rest`() {
    TestHttpServer(TestHttpServer.ranged(content)).use { storage ->
      server.handler = TestHttpServer.redirect(storage.url)
      partial().writeBytes(content.copyOf(100_000))

      val result = AssetDownloader().download(pin, target()) { false }

      assertEquals(DownloadResult.Installed, result)
      assertEquals(listOf<String?>("bytes=100000-"), server.ranges)
      assertEquals(listOf<String?>("bytes=100000-"), storage.ranges)
      assertContentEquals(content, target().readBytes())
    }
  }

  @Test
  fun `a server that stops sending is given up on after the read timeout`() {
    server.handler = TestHttpServer.stalling(content, sentBytes = 50_000, stallMillis = 3_000)
    val downloader = AssetDownloader(HttpRangeFetcher(readTimeoutMillis = 200))

    val result = downloader.download(pin, target()) { false }

    assertIs<SocketTimeoutException>(assertIs<DownloadResult.TransferFailed>(result).cause)
    assertContentEquals(content.copyOf(50_000), partial().readBytes())
  }
}
