package io.github.omochice.vvtts.voicevox

import io.github.omochice.vvtts.dictionary.DictionaryEntry
import jp.hiroshiba.voicevoxcore.UserDictWord
import jp.hiroshiba.voicevoxcore.blocking.UserDict

/**
 * A VOICEVOX user dictionary built from words, with how many of them did not go in as given.
 *
 * @property flattened how many words VOICEVOX took only without their accent.
 * @property skippedBecause for each word VOICEVOX refused even flat, the simple name of the
 *   exception that refused it.
 */
internal class BuiltUserDict(
    val userDict: UserDict,
    val flattened: Int,
    val skippedBecause: List<String>,
)

/**
 * Adds every one of [words] to a new user dictionary, flat when VOICEVOX refuses its accent, and
 * leaves out a word it refuses even then.
 */
internal fun buildUserDict(words: List<DictionaryEntry>): BuiltUserDict {
  val userDict = UserDict()
  var flattened = 0
  val skippedBecause = mutableListOf<String>()
  for (word in words) {
    val refusal = addTo(userDict, word, word.accent) ?: continue
    // Flat is accepted for every reading tried,
    // while the accepted accents end at VOICEVOX's own mora count,
    // which validation can only bound loosely, so a word is better read flat than not at all.
    if (word.accent == 0) {
      skippedBecause += refusal
    } else {
      addTo(userDict, word, 0)?.let { skippedBecause += it } ?: flattened++
    }
  }
  return BuiltUserDict(userDict, flattened, skippedBecause)
}

// A narrower catch would let a type nobody listed fail the request, and speaking without some of
// the user's words is better than not speaking.
/** Returns null when [entry] was added, otherwise the name of the exception that refused it. */
@Suppress("TooGenericExceptionCaught")
private fun addTo(userDict: UserDict, entry: DictionaryEntry, accent: Int): String? =
    try {
      // Not the default priority of 5, which was measured to leave a word the system dictionary
      // knows, 今日, read キョー instead of the registered コンニチ, while 10 overrode it.
      // A user registering a word means it should always be read this way.
      userDict.addWord(UserDictWord(entry.word, entry.reading, accent).priority(10))
      null
    } catch (e: Exception) {
      e.javaClass.simpleName
    }
