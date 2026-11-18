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
 * Speaks with the VOICEVOX styles of the voice models installed in [layout].
 *
 * Construction loads the native runtime and the dictionary, and the first [synthesize] with a voice
 * of a voice model loads that model and keeps it loaded; both are slow and may throw whatever
 * VOICEVOX CORE throws when an asset is missing or broken.
 *
 * Not thread-safe: callers must not use one instance from two threads at once.
 *
 * @param tempDir a writable directory OpenJTalk can use for its temporary files.
 */
class VoicevoxSpeechEngine(private val layout: AssetLayout, tempDir: File) : SpeechEngine {
  private val synthesizer: Synthesizer
  private val loadedVoiceModels = mutableSetOf<String>()

  init {
    // OpenJTalk creates temporary files under TMPDIR, which Android leaves unset.
    Os.setenv("TMPDIR", tempDir.absolutePath, true)
    val onnxRuntime =
        Onnxruntime.loadOnce().filename(layout.onnxRuntimeLibrary.absolutePath).perform()
    synthesizer =
        Synthesizer.builder(onnxRuntime, OpenJtalk(layout.dictionaryDir.absolutePath)).build()
  }

  override fun synthesize(chunk: String, voice: SpeakableVoice, prosody: Prosody): ByteArray {
    loadVoiceModelOf(voice)
    val query =
        try {
          synthesizer.createAudioQuery(chunk, voice.styleId)
        } catch (_: AnalyzeTextException) {
          return ByteArray(0)
        }
    query.outputSamplingRate = SpeechEngine.SAMPLE_RATE_HZ
    query.outputStereo = false
    query.speedScale = prosody.speedScale
    query.pitchScale = prosody.pitchScale
    return pcmFromWav(synthesizer.synthesis(query, voice.styleId).perform())
  }

  private fun loadVoiceModelOf(voice: SpeakableVoice) {
    val fileName = voice.voiceModelFileName
    if (fileName in loadedVoiceModels) return
    VoiceModelFile(layout.voiceModel(fileName).absolutePath).use {
      synthesizer.loadVoiceModel(it).perform()
    }
    loadedVoiceModels += fileName
  }
}
