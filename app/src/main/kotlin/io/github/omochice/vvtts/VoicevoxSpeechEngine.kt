package io.github.omochice.vvtts

import android.system.Os
import io.github.omochice.vvtts.assets.AssetLayout
import java.io.File
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import jp.hiroshiba.voicevoxcore.exceptions.AnalyzeTextException

/**
 * Speaks with one VOICEVOX style, using the assets installed in [layout].
 *
 * Construction loads the native runtime, the dictionary and the voice model, so it is slow and may
 * throw whatever VOICEVOX CORE throws when an asset is missing or broken.
 *
 * @param voiceModelFileName the installed voice model that contains [styleId], such as `0.vvm`.
 * @param tempDir a writable directory OpenJTalk can use for its temporary files.
 */
class VoicevoxSpeechEngine(
    layout: AssetLayout,
    voiceModelFileName: String,
    private val styleId: Int,
    tempDir: File,
) : SpeechEngine {
  private val synthesizer: Synthesizer

  init {
    // OpenJTalk creates temporary files under TMPDIR, which Android leaves unset.
    Os.setenv("TMPDIR", tempDir.absolutePath, true)
    val onnxRuntime =
        Onnxruntime.loadOnce().filename(layout.onnxRuntimeLibrary.absolutePath).perform()
    synthesizer =
        Synthesizer.builder(onnxRuntime, OpenJtalk(layout.dictionaryDir.absolutePath)).build()
    VoiceModelFile(layout.voiceModel(voiceModelFileName).absolutePath).use {
      synthesizer.loadVoiceModel(it).perform()
    }
  }

  override fun synthesize(chunk: String, prosody: Prosody): ByteArray {
    val query =
        try {
          synthesizer.createAudioQuery(chunk, styleId)
        } catch (_: AnalyzeTextException) {
          return ByteArray(0)
        }
    query.outputSamplingRate = SpeechEngine.SAMPLE_RATE_HZ
    query.outputStereo = false
    query.speedScale = prosody.speedScale
    query.pitchScale = prosody.pitchScale
    return pcmFromWav(synthesizer.synthesis(query, styleId).perform())
  }
}
