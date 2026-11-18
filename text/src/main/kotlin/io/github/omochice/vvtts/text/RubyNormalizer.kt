package io.github.omochice.vvtts.text

private const val KANJI = """[\p{IsHan}々〆ヵヶ]"""
private const val KANA = """[\p{IsHiragana}\p{IsKatakana}ー・]"""
private const val ANGLE_READING = """[^《》\r\n]+"""

// One alternation, rather than one pass per style, so that a reading produced by one style is
// never scanned again by another.
// The bar base excludes bars so that the nearest bar wins over an unrelated earlier one.
private val RUBY =
    Regex(
        """[｜|]([^《｜|\r\n]*)《($ANGLE_READING)》""" +
            """|($KANJI+)《($ANGLE_READING)》""" +
            """|($KANJI+)（($KANA+)）""" +
            """|($KANJI+)\(($KANA+)\)"""
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
        val groups = match.groupValues
        when {
          match.groups[2] != null -> groups[2]
          match.groups[4] != null -> groups[4]
          match.groups[5] != null -> parenthesised(match.value, groups[5], groups[6])
          else -> parenthesised(match.value, groups[7], groups[8])
        }
      }

  private fun parenthesised(original: String, base: String, reading: String): String =
      if (reading.length >= base.length) reading else original
}
