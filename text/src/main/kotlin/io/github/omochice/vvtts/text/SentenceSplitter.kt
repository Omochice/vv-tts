package io.github.omochice.vvtts.text

private const val DELIMITERS = "。！？!?\n"
private const val OPENERS = "「『（("
private const val CLOSERS = "」』）)"

/**
 * Splits text into sentence-sized chunks that are synthesized one at a time.
 *
 * Chunks end after 「。」「！」「？」「!」「?」 or a newline, never after 「、」.
 *
 * Sentence ends inside 「」『』（）() stay in one chunk unless the bracket content is too long.
 *
 * @param maxBracketContentLength the longest bracket content, in UTF-16 code units, that is kept
 *   whole; content any longer is split at its own sentence ends.
 * @throws IllegalArgumentException if [maxBracketContentLength] is negative.
 */
class SentenceSplitter(private val maxBracketContentLength: Int) {
  init {
    require(maxBracketContentLength >= 0) {
      "maxBracketContentLength must not be negative: $maxBracketContentLength"
    }
  }

  /**
   * Returns the chunks of [text] in reading order, trimmed of surrounding whitespace.
   *
   * Every chunk has something to read, so each can be handed to the synthesizer.
   */
  fun split(text: String): List<String> {
    val sheltered = shelteredPositions(text)
    val chunks = mutableListOf<String>()
    var start = 0
    var i = 0
    while (i < text.length) {
      if (!sheltered[i] && text[i] in DELIMITERS) {
        val end = endOfTrailingRun(text, i + 1)
        chunks += text.substring(start, end)
        start = end
        i = end
      } else {
        i++
      }
    }
    chunks += text.substring(start)
    // Bracket-only fragments are dropped rather than merged into a neighbour.
    // They carry nothing to read, and merging would pull a newline or a page-cut remnant into a
    // spoken chunk.
    return chunks
        .map { it.trim() }
        .filter { chunk -> chunk.any { it !in OPENERS && it !in CLOSERS } }
  }

  // Pairing looks ahead over the whole input rather than a bounded window.
  // A window would be a second length to tune alongside the content threshold.
  // A far-off unrelated closer can still pair with an unclosed opener, but the threshold then
  // releases the content anyway.
  private fun shelteredPositions(text: String): BooleanArray {
    val sheltered = BooleanArray(text.length)
    val openers = ArrayDeque<Int>()
    for (i in text.indices) {
      if (text[i] in OPENERS) openers.addLast(i)
      val kind = CLOSERS.indexOf(text[i])
      if (kind < 0) continue
      // Searching below the top lets 「（」 still pair 「 with 」.
      // Popping only the top would let the stray （ steal the 」 and leave 「 unclosed.
      val depth = openers.indexOfLast { text[it] == OPENERS[kind] }
      if (depth < 0) continue
      val open = openers[depth]
      while (openers.size > depth) openers.removeLast()
      if (i - open - 1 <= maxBracketContentLength) {
        for (j in open + 1 until i) sheltered[j] = true
      }
    }
    return sheltered
  }

  private fun endOfTrailingRun(text: String, from: Int): Int {
    var end = from
    while (end < text.length && (text[end] in DELIMITERS || text[end] in CLOSERS)) end++
    return end
  }
}
