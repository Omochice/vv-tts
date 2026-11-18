package io.github.omochice.vvtts

import android.speech.tts.TextToSpeech
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageSupportTest {
  @Test
  fun `Japanese in Japan is supported down to the country`() {
    assertEquals(LanguageSupport.LANGUAGE_AND_COUNTRY, LanguageSupport.of("jpn", "JPN"))
  }

  @Test
  fun `Japanese with another country is supported only as a language`() {
    assertEquals(LanguageSupport.LANGUAGE, LanguageSupport.of("jpn", "USA"))
  }

  @Test
  fun `Japanese without a country is supported only as a language`() {
    assertEquals(LanguageSupport.LANGUAGE, LanguageSupport.of("jpn", ""))
    assertEquals(LanguageSupport.LANGUAGE, LanguageSupport.of("jpn", null))
  }

  @Test
  fun `languages other than Japanese are not supported`() {
    assertEquals(LanguageSupport.NONE, LanguageSupport.of("eng", "JPN"))
    assertEquals(LanguageSupport.NONE, LanguageSupport.of("", "JPN"))
    assertEquals(LanguageSupport.NONE, LanguageSupport.of(null, null))
  }

  @Test
  fun `two-letter codes are not accepted because the framework passes three-letter codes`() {
    assertEquals(LanguageSupport.NONE, LanguageSupport.of("ja", "JP"))
  }

  @Test
  fun `a supported locale is available down to the level it matches while a voice is installed`() {
    assertEquals(
        TextToSpeech.LANG_COUNTRY_AVAILABLE,
        LanguageSupport.LANGUAGE_AND_COUNTRY.ttsResult(isAnyVoiceInstalled = true),
    )
    assertEquals(
        TextToSpeech.LANG_AVAILABLE,
        LanguageSupport.LANGUAGE.ttsResult(isAnyVoiceInstalled = true),
    )
  }

  @Test
  fun `a supported locale is missing data while no voice is installed`() {
    assertEquals(
        TextToSpeech.LANG_MISSING_DATA,
        LanguageSupport.LANGUAGE_AND_COUNTRY.ttsResult(isAnyVoiceInstalled = false),
    )
    assertEquals(
        TextToSpeech.LANG_MISSING_DATA,
        LanguageSupport.LANGUAGE.ttsResult(isAnyVoiceInstalled = false),
    )
  }

  @Test
  fun `an unsupported locale is not supported whether or not a voice is installed`() {
    assertEquals(
        TextToSpeech.LANG_NOT_SUPPORTED,
        LanguageSupport.NONE.ttsResult(isAnyVoiceInstalled = true),
    )
    assertEquals(
        TextToSpeech.LANG_NOT_SUPPORTED,
        LanguageSupport.NONE.ttsResult(isAnyVoiceInstalled = false),
    )
  }
}
