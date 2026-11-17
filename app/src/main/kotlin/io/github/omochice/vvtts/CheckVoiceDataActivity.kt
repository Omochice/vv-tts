package io.github.omochice.vvtts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/**
 * Answers the framework's voice-data check so system Settings enables the engine's sample playback.
 */
class CheckVoiceDataActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Settings compares "jpn-JPN" verbatim against the default locale, so the
    // ISO 639-2/3166 alpha-3 spelling is required rather than a BCP 47 tag.
    setResult(
        TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,
        Intent().apply {
          putStringArrayListExtra(
              TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
              arrayListOf("jpn", "jpn-JPN"),
          )
          putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf())
        },
    )
    finish()
  }
}
