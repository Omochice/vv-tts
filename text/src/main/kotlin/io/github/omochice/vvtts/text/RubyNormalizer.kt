package io.github.omochice.vvtts.text

private const val KANJI = """[\p{IsHan}々〆ヵヶ]"""
private const val KANA = """[\p{IsHiragana}\p{IsKatakana}ー・]"""
private const val ANGLE_READING = """[^《》\r\n]+"""

// One alternation, rather than one pass per style, so that a reading produced by one style is
// never scanned again by another.
// The bar base excludes bars so that the nearest bar wins over an unrelated earlier one.
private val RUBY =
    Regex(
        """[｜|]([^《｜|\r\n]*)《(?<barReading>$ANGLE_READING)》""" +
            """|($KANJI+)《(?<angleReading>$ANGLE_READING)》""" +
            """|(?<wideBase>$KANJI+)（(?<wideReading>$KANA+)）""" +
            """|(?<narrowBase>$KANJI+)\((?<narrowReading>$KANA+)\)"""
    )

/**
 * Replaces inline ruby annotations with the reading alone.
 *
 * Ruby is written where the reading of the kanji is not obvious, so the reading is what is spoken.
 */
object RubyNormalizer {
  /**
   * Returns [text] with every `｜base《reading》`, `base《reading》`, `base（reading）` and
   * `base(reading)` replaced by its reading.
   *
   * Brackets that do not form a ruby annotation, such as `彼（主人公）`, are kept as they are.
   */
  fun normalize(text: String): String =
      RUBY.replace(text) { match ->
        when {
          match.groups["barReading"] != null -> match.valueOf("barReading")
          match.groups["angleReading"] != null -> match.valueOf("angleReading")
          match.groups["wideBase"] != null ->
              parenthesised(match.value, match.valueOf("wideBase"), match.valueOf("wideReading"))
          else ->
              parenthesised(
                  match.value,
                  match.valueOf("narrowBase"),
                  match.valueOf("narrowReading"),
              )
        }
      }

  private fun MatchResult.valueOf(group: String): String = groups[group]?.value.orEmpty()

  private fun parenthesised(original: String, base: String, reading: String): String =
      if (reading.length >= base.length) reading else original
}
