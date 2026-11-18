package io.github.omochice.vvtts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.text.SentenceSplitter
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "VvTts"

// Provisional until request logs from real e-book readers show typical bracket lengths.
// It lives here alone so tuning it later touches a single place.
private const val PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH = 100

/** The system text-to-speech engine that reads Japanese text aloud. */
class VvTtsService : TextToSpeechService() {
  private val splitter = SentenceSplitter(PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH)
  private var loop: SynthesisLoop? = null
  private val stopRequested = AtomicBoolean(false)

  override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
      LanguageSupport.of(lang, country).toTtsResult()

  override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
      onIsLanguageAvailable(lang, country, variant)

  override fun onGetLanguage(): Array<String> = arrayOf("jpn", "JPN", "")

  override fun onStop() {
    stopRequested.set(true)
  }

  override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
    stopRequested.set(false)
    val text = request.charSequenceText?.toString().orEmpty()
    val chunks = splitter.split(text)
    logRequest(request, text, chunks)
    speak(request, chunks, callback)
    // Without done(), the framework never tells the client about an error raised before start().
    callback.done()
  }

  private fun speak(request: SynthesisRequest, chunks: List<String>, callback: SynthesisCallback) {
    if (LanguageSupport.of(request.language, request.country) == LanguageSupport.NONE) {
      Log.w(TAG, "unsupported locale ${request.language}-${request.country}")
      callback.error()
      return
    }
    val layout = AssetLayout(filesDir)
    if (!layout.isInstalled(PROVISIONAL_VOICE_MODEL_FILE_NAME)) {
      Log.w(TAG, "speech assets are not installed")
      callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
      return
    }
    val loop =
        try {
          loopFor(layout)
        } catch (e: Exception) {
          Log.e(TAG, "cannot start the speech engine", e)
          callback.error(TextToSpeech.ERROR_SYNTHESIS)
          return
        }
    callback.start(SpeechEngine.SAMPLE_RATE_HZ, AudioFormat.ENCODING_PCM_16BIT, 1)
    try {
      loop.run(chunks, CallbackSink(callback)) { stopRequested.get() }
    } catch (e: Exception) {
      Log.e(TAG, "synthesis failed", e)
      callback.error(TextToSpeech.ERROR_SYNTHESIS)
    }
  }

  private fun loopFor(layout: AssetLayout): SynthesisLoop =
      loop
          ?: SynthesisLoop(
                  VoicevoxSpeechEngine(
                      layout,
                      PROVISIONAL_VOICE_MODEL_FILE_NAME,
                      PROVISIONAL_STYLE_ID,
                      cacheDir,
                  )
              )
              .also { loop = it }

  private fun logRequest(request: SynthesisRequest, text: String, chunks: List<String>) {
    Log.i(
        TAG,
        "request textLength=${text.length} chunks=${chunks.size} " +
            "chunkLengths=${chunks.map { it.length }} " +
            "locale=${request.language}/${request.country}/${request.variant} " +
            "voice=${request.voiceName} rate=${request.speechRate} pitch=${request.pitch}",
    )
    if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "text=$text")
  }

  private class CallbackSink(private val callback: SynthesisCallback) : PcmSink {
    override val maxWriteBytes: Int = callback.maxBufferSize

    override fun write(pcm: ByteArray, offset: Int, length: Int): Boolean =
        callback.audioAvailable(pcm, offset, length) == TextToSpeech.SUCCESS
  }
}

private fun LanguageSupport.toTtsResult(): Int =
    when (this) {
      LanguageSupport.LANGUAGE_AND_COUNTRY -> TextToSpeech.LANG_COUNTRY_AVAILABLE
      LanguageSupport.LANGUAGE -> TextToSpeech.LANG_AVAILABLE
      LanguageSupport.NONE -> TextToSpeech.LANG_NOT_SUPPORTED
    }
