package io.github.omochice.vvtts.voicevox

import io.github.omochice.vvtts.dictionary.DictionaryEntry
import jp.hiroshiba.voicevoxcore.UserDictWord
import jp.hiroshiba.voicevoxcore.blocking.UserDict

/**
 * A VOICEVOX user dictionary built from words, with the words it refused.
 *
 * @property skippedBecause for each refused word, the simple name of the exception that refused it.
 */
internal class BuiltUserDict(val userDict: UserDict, val skippedBecause: List<String>)

/** Adds every one of [words] VOICEVOX accepts to a new user dictionary. */
internal fun buildUserDict(words: List<DictionaryEntry>): BuiltUserDict {
  val userDict = UserDict()
  return BuiltUserDict(userDict, words.mapNotNull { addTo(userDict, it) })
}

// A narrower catch would let a type nobody listed fail the request, and speaking without some of
// the user's words is better than not speaking.
/** Returns null when [entry] was added, otherwise the name of the exception that refused it. */
@Suppress("TooGenericExceptionCaught")
private fun addTo(userDict: UserDict, entry: DictionaryEntry): String? =
    try {
      // Not the default priority of 5, which was measured to leave a word the system dictionary
      // knows, 今日, read キョー instead of the registered コンニチ, while 10 overrode it.
      // A user registering a word means it should always be read this way.
      // Accent type 0 is accepted for any reading, whereas a real accent has to be checked
      // against VOICEVOX's mora rules, so accent input is left out for now.
      userDict.addWord(UserDictWord(entry.word, entry.reading, 0).priority(10))
      null
    } catch (e: Exception) {
      e.javaClass.simpleName
    }
