package io.github.omochice.vvtts.voicevox

import android.system.Os
import android.util.Log
import io.github.omochice.vvtts.Prosody
import io.github.omochice.vvtts.SpeakableVoice
import io.github.omochice.vvtts.SpeechEngine
import io.github.omochice.vvtts.TAG
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.dictionary.DictionaryChanges
import io.github.omochice.vvtts.dictionary.wordsToRegister
import io.github.omochice.vvtts.pcmFromWav
import java.io.File
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import jp.hiroshiba.voicevoxcore.exceptions.AnalyzeTextException

/**
 * Speaks with the VOICEVOX styles of the voice models installed in [layout], reading the words of
 * the user dictionary as [dictionary] reports them.
 *
 * Construction loads the native runtime and the dictionary, and the first [synthesize] with a voice
 * of a voice model loads that model and keeps it loaded; both are slow and may throw whatever
 * VOICEVOX CORE throws when an asset is missing or broken. A user dictionary that cannot be applied
 * never fails construction or [synthesize].
 *
 * Not thread-safe: callers must not use one instance from two threads at once.
 *
 * @param tempDir a writable directory OpenJTalk can use for its temporary files.
 */
class VoicevoxSpeechEngine(
    private val layout: AssetLayout,
    tempDir: File,
    private val dictionary: DictionaryChanges,
) : SpeechEngine {
  private val openJtalk: OpenJtalk
  private val synthesizer: Synthesizer
  private val loadedVoiceModels = mutableSetOf<String>()

  init {
    // OpenJTalk creates temporary files under TMPDIR, which Android leaves unset.
    Os.setenv("TMPDIR", tempDir.absolutePath, true)
    val onnxRuntime =
        Onnxruntime.loadOnce().filename(layout.onnxRuntimeLibrary.absolutePath).perform()
    openJtalk = OpenJtalk(layout.dictionaryDir.absolutePath)
    synthesizer = Synthesizer.builder(onnxRuntime, openJtalk).build()
    applyDictionaryIfChanged()
  }

  override fun synthesize(chunk: String, voice: SpeakableVoice, prosody: Prosody): ByteArray {
    applyDictionaryIfChanged()
    loadVoiceModelOf(voice)
    val query =
        try {
          synthesizer.createAudioQuery(chunk, voice.styleId)
        } catch (_: AnalyzeTextException) {
          return ByteArray(0)
        }
    if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "kana=${query.kana}")
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

  // A narrower catch would let a type nobody listed fail the request, and speaking without some of
  // the user's words is better than not speaking.
  // Exceptions are logged by type only, because VOICEVOX's messages quote the word, and the log
  // must not carry the user's text.
  @Suppress("TooGenericExceptionCaught")
  private fun applyDictionaryIfChanged() {
    val entries =
        try {
          dictionary.changedEntries() ?: return
        } catch (e: Exception) {
          Log.w(TAG, "cannot read the user dictionary: ${e.javaClass.name}")
          return
        }
    val words = wordsToRegister(entries)
    val built = buildUserDict(words)
    val failures = built.skippedBecause
    try {
      openJtalk.useUserDict(built.userDict)
      Log.i(
          TAG,
          "user dictionary entries=${entries.size} words=${words.size} " +
              "applied=${words.size - failures.size} flattened=${built.flattened} " +
              "skipped=${failures.size} " +
              "skippedBecause=${failures.distinct()}",
      )
    } catch (e: Exception) {
      Log.w(TAG, "cannot apply the user dictionary words=${words.size}: ${e.javaClass.name}")
    }
  }
}
