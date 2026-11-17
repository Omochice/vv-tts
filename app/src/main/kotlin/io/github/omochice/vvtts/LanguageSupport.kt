package io.github.omochice.vvtts

private const val JAPANESE = "jpn"
private const val JAPAN = "JPN"

/** How closely a requested locale matches what this engine can speak. */
enum class LanguageSupport {
  LANGUAGE_AND_COUNTRY,
  LANGUAGE,
  NONE;

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
