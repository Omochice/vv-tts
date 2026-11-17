package io.github.omochice.vvtts

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
}
