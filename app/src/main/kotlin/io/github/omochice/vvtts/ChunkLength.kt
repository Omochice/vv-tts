package io.github.omochice.vvtts

import io.github.omochice.vvtts.text.RubyNormalizer
import io.github.omochice.vvtts.text.SentenceSplitter
import io.github.omochice.vvtts.text.limitLength

// Provisional: one chunk took 780 MB for 100 characters at normal speed, and 276 characters at a
// speedScale of 0.62 got the process killed at 2.4 GB.
private const val PROVISIONAL_MAX_CHUNK_LENGTH_AT_NORMAL_SPEED = 80

// Without a floor, a Prosody slower than fromAndroid allows would cut words into a few characters
// each and, below 2, make limitLength throw.
private const val PROVISIONAL_MIN_MAX_CHUNK_LENGTH = 20

/**
 * The longest chunk, in UTF-16 code units, that is synthesized in one call at [prosody].
 *
 * It shrinks with the speed because slower speech voices more audio per character.
 */
internal fun maxChunkLength(prosody: Prosody): Int =
    (PROVISIONAL_MAX_CHUNK_LENGTH_AT_NORMAL_SPEED * prosody.speedScale)
        .toInt()
        .coerceAtLeast(PROVISIONAL_MIN_MAX_CHUNK_LENGTH)

// Provisional until request logs from real e-book readers show typical bracket lengths.
// It lives here alone so tuning it later touches a single place.
private const val PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH = 100

private val splitter = SentenceSplitter(PROVISIONAL_MAX_BRACKET_CONTENT_LENGTH)

/**
 * Returns the chunks of [text] in reading order, with ruby normalized, each no longer than
 * [maxChunkLength] at [prosody].
 */
internal fun speechChunks(text: String, prosody: Prosody): List<String> {
  val maxLength = maxChunkLength(prosody)
  return splitter.split(RubyNormalizer.normalize(text)).flatMap { limitLength(it, maxLength) }
}
