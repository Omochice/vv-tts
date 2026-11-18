package io.github.omochice.vvtts.assets

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class AssetLayoutTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun layout() = AssetLayout(tmp.root)

  private fun File.create() = apply {
    parentFile.mkdirs()
    writeText("x")
  }

  private fun installEverything() {
    val layout = layout()
    layout.onnxRuntimeLibrary.create()
    File(layout.dictionaryDir, "sys.dic").create()
    layout.voiceModel("0.vvm").create()
  }

  @Test
  fun `assets live at the paths the extracted archives and downloads produce`() {
    val layout = layout()

    assertEquals(File(tmp.root, "ort/lib/libvoicevox_onnxruntime.so"), layout.onnxRuntimeLibrary)
    assertEquals(File(tmp.root, "dict"), layout.dictionaryDir)
    assertEquals(File(tmp.root, "vvm/3.vvm"), layout.voiceModel("3.vvm"))
  }

  @Test
  fun `the library sits inside the ONNX Runtime directory and downloads have their own directory`() {
    val layout = layout()

    assertEquals(File(tmp.root, "ort"), layout.onnxRuntimeDir)
    assertEquals(File(tmp.root, "downloads"), layout.downloadsDir)
  }

  @Test
  fun `terms documents have their own directory`() {
    assertEquals(File(tmp.root, "terms"), layout().termsDir)
  }

  @Test
  fun `a root with the library, a populated dictionary and the voice model is installed`() {
    installEverything()

    assertTrue(layout().isInstalled("0.vvm"))
  }

  @Test
  fun `an empty root is not installed`() {
    assertFalse(layout().isInstalled("0.vvm"))
  }

  @Test
  fun `a missing library means not installed`() {
    installEverything()
    layout().onnxRuntimeLibrary.delete()

    assertFalse(layout().isInstalled("0.vvm"))
  }

  @Test
  fun `an empty dictionary directory means not installed`() {
    installEverything()
    File(layout().dictionaryDir, "sys.dic").delete()

    assertFalse(layout().isInstalled("0.vvm"))
  }

  @Test
  fun `a dictionary path that is a file means not installed`() {
    installEverything()
    layout().dictionaryDir.deleteRecursively()
    layout().dictionaryDir.create()

    assertFalse(layout().isInstalled("0.vvm"))
  }

  @Test
  fun `a voice model other than the requested one does not count`() {
    installEverything()

    assertFalse(layout().isInstalled("1.vvm"))
  }

  @Test
  fun `the library and a populated dictionary are the base parts without any voice model`() {
    installEverything()
    layout().voiceModel("0.vvm").delete()

    assertTrue(layout().hasBaseParts())
  }

  @Test
  fun `a missing library means the base parts are incomplete`() {
    installEverything()
    layout().onnxRuntimeLibrary.delete()

    assertFalse(layout().hasBaseParts())
  }

  @Test
  fun `an empty dictionary directory means the base parts are incomplete`() {
    installEverything()
    File(layout().dictionaryDir, "sys.dic").delete()

    assertFalse(layout().hasBaseParts())
  }

  @Test
  fun `a directory in place of the library does not count`() {
    installEverything()
    layout().onnxRuntimeLibrary.delete()
    layout().onnxRuntimeLibrary.mkdirs()

    assertFalse(layout().isInstalled("0.vvm"))
  }
}
