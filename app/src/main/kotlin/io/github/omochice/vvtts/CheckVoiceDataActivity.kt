package io.github.omochice.vvtts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetLayout

/**
 * Answers the framework's voice-data check so system Settings enables the engine's sample playback.
 */
class CheckVoiceDataActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Settings compares "jpn-JPN" verbatim against the default locale, so the
    // ISO 639-2/3166 alpha-3 spelling is required rather than a BCP 47 tag.
    val voices = arrayListOf(JAPANESE, "$JAPANESE-$JAPAN")
    val installed =
        installedVoices(AssetCatalog.load().voiceModels, AssetLayout(filesDir)).isNotEmpty()
    setResult(
        if (installed) TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
        else TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL,
        Intent().apply {
          putStringArrayListExtra(
              TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
              if (installed) voices else arrayListOf(),
          )
          putStringArrayListExtra(
              TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,
              if (installed) arrayListOf() else voices,
          )
        },
    )
    finish()
  }
}
