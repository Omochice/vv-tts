package io.github.omochice.vvtts.voicevox

import jp.hiroshiba.voicevoxcore.UserDictWord
import jp.hiroshiba.voicevoxcore.blocking.UserDict
import jp.hiroshiba.voicevoxcore.exceptions.InvalidWordException

// Any surface would do; the probe dictionary is thrown away and never applied.
private const val PROBE_SURFACE = "zz"

/**
 * Tells whether [accent] is the highest accent VOICEVOX's user dictionary accepts for [reading],
 * that is, whether VOICEVOX counts [accent] moras in it.
 */
internal fun isHighestAcceptedAccent(reading: String, accent: Int): Boolean =
    accepts(reading, accent) && !accepts(reading, accent + 1)

// Only the refusal of an out-of-range accent answers the question; anything else means the engine
// is unusable and reaches the caller.
private fun accepts(reading: String, accent: Int): Boolean =
    try {
      UserDict().addWord(UserDictWord(PROBE_SURFACE, reading, accent))
      true
    } catch (_: InvalidWordException) {
      false
    }
