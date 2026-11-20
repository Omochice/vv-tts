package io.github.omochice.vvtts

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
