package io.github.omochice.vvtts

// Provisional, from peak memory measured on one emulator and one Pixel 9a.
// One chunk took about 250 MB plus 5 to 7 MB per character / speedScale, and 276 characters at a
// speedScale of 0.62 got the process killed at 2.4 GB.
// Bounding character / speedScale by 80 keeps one chunk below the 780 MB measured for 100
// characters at normal speed.
// They live here alone so tuning them later touches a single place.
private const val PROVISIONAL_MAX_CHUNK_LENGTH_AT_NORMAL_SPEED = 80

// Android puts no lower bound on the speech rate, and a cap that shrank with it would cut words
// into a few characters each.
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
