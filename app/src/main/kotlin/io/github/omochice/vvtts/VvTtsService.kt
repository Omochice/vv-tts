package io.github.omochice.vvtts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.text.RubyNormalizer
import io.github.omochice.vvtts.text.SentenceSplitter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

internal const val TAG = "VvTts"

// Provisional until request logs from real e-book readers show typical bracket lengths.
// It lives here alone so tuning it later touches a single place.
private const val PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH = 100

/** The system text-to-speech engine that reads Japanese text aloud. */
class VvTtsService : TextToSpeechService() {
  private val splitter = SentenceSplitter(PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH)
  private val catalog by lazy { AssetCatalog.load() }
  private val layout by lazy { AssetLayout(filesDir) }
  private val preferredVoice by lazy { PreferredVoiceStore(this) }
  private var loop: SynthesisLoop? = null
  private val stopRequested = AtomicBoolean(false)

  override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
      LanguageSupport.of(lang, country)
          .ttsResult(isAnyVoiceInstalled = currentVoices().isNotEmpty())

  override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
      onIsLanguageAvailable(lang, country, variant)

  override fun onGetLanguage(): Array<String> = arrayOf("jpn", "JPN", "")

  override fun onGetVoices(): List<Voice> =
      currentVoices().map {
        Voice(
            it.name,
            Locale.JAPAN,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            false,
            emptySet(),
        )
      }

  override fun onIsValidVoiceName(voiceName: String?): Int =
      if (currentVoices().any { it.name == voiceName }) TextToSpeech.SUCCESS else TextToSpeech.ERROR

  override fun onLoadVoice(voiceName: String?): Int = onIsValidVoiceName(voiceName)

  override fun onGetDefaultVoiceNameFor(
      lang: String?,
      country: String?,
      variant: String?,
  ): String? =
      if (LanguageSupport.of(lang, country) == LanguageSupport.NONE) null
      else resolveVoice(null, preferredVoice.get(), currentVoices())?.name

  override fun onStop() {
    stopRequested.set(true)
  }

  override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
    stopRequested.set(false)
    val text = request.charSequenceText?.toString().orEmpty()
    val chunks = splitter.split(RubyNormalizer.normalize(text))
    val voice = resolveVoice(request.voiceName, preferredVoice.get(), currentVoices())
    logRequest(request, text, chunks, voice)
    speak(request, voice, chunks, callback)
    // Without done(), the framework never tells the client about an error raised before start().
    callback.done()
  }

  private fun currentVoices(): List<SpeakableVoice> = installedVoices(catalog.voiceModels, layout)

  private fun speak(
      request: SynthesisRequest,
      voice: SpeakableVoice?,
      chunks: List<String>,
      callback: SynthesisCallback,
  ) {
    if (LanguageSupport.of(request.language, request.country) == LanguageSupport.NONE) {
      Log.w(TAG, "unsupported locale ${request.language}-${request.country}")
      callback.error()
      return
    }
    if (voice == null) {
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
    // The framework can stop a request before onSynthesizeText runs.
    val started = callback.start(SpeechEngine.SAMPLE_RATE_HZ, AudioFormat.ENCODING_PCM_16BIT, 1)
    if (started != TextToSpeech.SUCCESS) return
    try {
      loop.run(
          chunks,
          voice,
          Prosody.fromAndroid(request.speechRate, request.pitch),
          CallbackSink(callback),
      ) {
        stopRequested.get()
      }
    } catch (e: Exception) {
      Log.e(TAG, "synthesis failed", e)
      callback.error(TextToSpeech.ERROR_SYNTHESIS)
    }
  }

  private fun loopFor(layout: AssetLayout): SynthesisLoop =
      loop ?: SynthesisLoop(VoicevoxSpeechEngine(layout, cacheDir)).also { loop = it }

  private fun logRequest(
      request: SynthesisRequest,
      text: String,
      chunks: List<String>,
      voice: SpeakableVoice?,
  ) {
    Log.i(
        TAG,
        "request textLength=${text.length} chunks=${chunks.size} " +
            "chunkLengths=${chunks.map { it.length }} " +
            "locale=${request.language}/${request.country}/${request.variant} " +
            "voice=${request.voiceName} resolvedVoice=${voice?.name} " +
            "rate=${request.speechRate} pitch=${request.pitch}",
    )
    if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "text=$text")
  }

  private class CallbackSink(private val callback: SynthesisCallback) : PcmSink {
    override val maxWriteBytes: Int = callback.maxBufferSize

    override fun write(pcm: ByteArray, offset: Int, length: Int): Boolean =
        callback.audioAvailable(pcm, offset, length) == TextToSpeech.SUCCESS
  }
}
