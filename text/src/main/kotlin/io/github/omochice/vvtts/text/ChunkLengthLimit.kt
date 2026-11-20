package io.github.omochice.vvtts.text

// Without 「；」, which is rare in Japanese prose, and whitespace, which often sits inside a phrase
// such as "Android 15", where a cut would not fall on a pause.
private const val SOFT_BREAKS = "、，,"

private const val MIN_MAX_LENGTH = 2

/**
 * Breaks [chunk] into pieces no longer than [maxLength], in reading order.
 *
 * A longer chunk is cut after the last 「、」「，」「,」 that fits, which stays at the end of the earlier
 * piece, or at [maxLength] itself when none fits, never between the halves of a surrogate pair.
 *
 * The synthesizer voices each cut as the end of a sentence.
 *
 * @param maxLength the longest piece, in UTF-16 code units.
 * @return the pieces, trimmed of surrounding whitespace, none of them empty.
 * @throws IllegalArgumentException if [maxLength] is below 2, which leaves no room to keep a
 *   surrogate pair whole.
 */
fun limitLength(chunk: String, maxLength: Int): List<String> {
  require(maxLength >= MIN_MAX_LENGTH) {
    "maxLength must be at least $MIN_MAX_LENGTH: $maxLength"
  }
  val pieces = mutableListOf<String>()
  var rest = chunk.trim()
  while (rest.length > maxLength) {
    val cut = cutPosition(rest, maxLength)
    pieces += rest.substring(0, cut).trim()
    rest = rest.substring(cut).trim()
  }
  pieces += rest
  return pieces.filter { it.isNotEmpty() }
}

private fun cutPosition(text: String, maxLength: Int): Int {
  val softBreak = text.lastIndexOfAny(SOFT_BREAKS.toCharArray(), startIndex = maxLength - 1)
  if (softBreak >= 0) return softBreak + 1
  return if (Character.isSurrogatePair(text[maxLength - 1], text[maxLength])) maxLength - 1
  else maxLength
}
