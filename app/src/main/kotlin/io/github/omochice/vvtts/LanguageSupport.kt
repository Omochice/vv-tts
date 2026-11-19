package io.github.omochice.vvtts

import android.speech.tts.TextToSpeech

internal const val JAPANESE = "jpn"
internal const val JAPAN = "JPN"

/** How closely a requested locale matches what this engine can speak. */
enum class LanguageSupport {
  /** Both the language and the country of the locale are spoken. */
  LANGUAGE_AND_COUNTRY,

  /** The language of the locale is spoken, but not for its country. */
  LANGUAGE,

  /** The language of the locale is not spoken. */
  NONE;

  /**
   * Returns the `TextToSpeech.LANG_*` code that reports this support to the framework.
   *
   * @param isAnyVoiceInstalled whether the engine can speak at all; while it cannot, a supported
   *   locale is reported as missing data.
   */
  fun ttsResult(isAnyVoiceInstalled: Boolean): Int =
      when {
        this == NONE -> TextToSpeech.LANG_NOT_SUPPORTED
        !isAnyVoiceInstalled -> TextToSpeech.LANG_MISSING_DATA
        this == LANGUAGE_AND_COUNTRY -> TextToSpeech.LANG_COUNTRY_AVAILABLE
        else -> TextToSpeech.LANG_AVAILABLE
      }

  companion object {
    /**
     * Returns the support for a locale given as ISO 639-2 [language] and ISO 3166 alpha-3 [country]
     * codes, as the TTS framework passes them.
     */
    fun of(language: String?, country: String?): LanguageSupport =
        when {
          language != JAPANESE -> NONE
          country == JAPAN -> LANGUAGE_AND_COUNTRY
          else -> LANGUAGE
        }
  }
}
