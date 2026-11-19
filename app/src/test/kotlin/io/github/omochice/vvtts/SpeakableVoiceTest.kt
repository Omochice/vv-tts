package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.assets.RemoteFile
import io.github.omochice.vvtts.assets.Voice
import io.github.omochice.vvtts.assets.VoiceModel
import io.github.omochice.vvtts.assets.create
import io.github.omochice.vvtts.assets.installBasePartsByHand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class SpeakableVoiceTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun model(fileName: String, vararg voices: Voice) =
      VoiceModel(
          RemoteFile(fileName, "https://example.invalid/$fileName", 1, "0".repeat(64)),
          voices.toList(),
      )

  private val models =
      listOf(
          model("0.vvm", Voice("四国めたん", "ノーマル", 2), Voice("ずんだもん", "ノーマル", 3)),
          model("1.vvm", Voice("冥鳴ひまり", "ノーマル", 14)),
          model("5.vvm", Voice("ずんだもん", "ささやき", 22)),
      )

  private val metan = SpeakableVoice("四国めたん（ノーマル）", "0.vvm", 2)
  private val zundamon = SpeakableVoice("ずんだもん（ノーマル）", "0.vvm", 3)
  private val himari = SpeakableVoice("冥鳴ひまり（ノーマル）", "1.vvm", 14)
  private val whisper = SpeakableVoice("ずんだもん（ささやき）", "5.vvm", 22)

  @Test
  fun `a voice is named by its character and its style in full-width parentheses`() {
    assertEquals(
        SpeakableVoice("ずんだもん（ノーマル）", "0.vvm", 3),
        SpeakableVoice.of(Voice("ずんだもん", "ノーマル", 3), "0.vvm"),
    )
  }

  @Test
  fun `every voice of every installed voice model is speakable in catalog order`() {
    assertEquals(listOf(metan, zundamon, himari, whisper), speakableVoices(models) { true })
  }

  @Test
  fun `voices of a voice model that is not installed are not speakable`() {
    assertEquals(listOf(metan, zundamon, whisper), speakableVoices(models) { it != "1.vvm" })
  }

  @Test
  fun `nothing is speakable when no voice model is installed`() {
    assertTrue(speakableVoices(models) { false }.isEmpty())
  }

  @Test
  fun `every voice in the bundled catalog has a name of its own`() {
    val names = speakableVoices(AssetCatalog.load().voiceModels) { true }.map { it.name }

    assertEquals(128, names.size)
    assertEquals(names.size, names.toSet().size)
    assertTrue("ずんだもん（ノーマル）" in names)
  }

  @Test
  fun `the requested voice is spoken when it is speakable`() {
    assertEquals(
        himari,
        resolveVoice("冥鳴ひまり（ノーマル）", zundamon.name, listOf(metan, zundamon, himari)),
    )
  }

  @Test
  fun `the preferred voice is spoken when the request names no voice`() {
    assertEquals(zundamon, resolveVoice(null, zundamon.name, listOf(metan, zundamon)))
    assertEquals(zundamon, resolveVoice("", zundamon.name, listOf(metan, zundamon)))
  }

  @Test
  fun `the preferred voice is spoken when the requested voice is not speakable`() {
    assertEquals(zundamon, resolveVoice(himari.name, zundamon.name, listOf(metan, zundamon)))
  }

  @Test
  fun `a framework locale name in the request falls back like an unknown voice`() {
    assertEquals(zundamon, resolveVoice("ja-JP", zundamon.name, listOf(metan, zundamon)))
  }

  @Test
  fun `the first speakable voice is spoken when no voice is preferred`() {
    assertEquals(metan, resolveVoice(null, null, listOf(metan, zundamon)))
  }

  @Test
  fun `the first speakable voice is spoken when the preferred voice is not speakable`() {
    assertEquals(metan, resolveVoice(himari.name, himari.name, listOf(metan, zundamon)))
  }

  @Test
  fun `nothing is spoken when no voice is speakable`() {
    assertNull(resolveVoice(zundamon.name, zundamon.name, emptyList()))
  }

  @Test
  fun `the installed voices are those of the voice models present beside the base parts`() {
    val layout = AssetLayout(tmp.root)
    layout.installBasePartsByHand()
    layout.voiceModel("1.vvm").create()

    assertEquals(listOf(himari), installedVoices(models, layout))
  }

  @Test
  fun `no voice is installed while a base part is missing`() {
    val layout = AssetLayout(tmp.root)
    layout.onnxRuntimeLibrary.create()
    layout.voiceModel("1.vvm").create()

    assertTrue(installedVoices(models, layout).isEmpty())
  }
}
