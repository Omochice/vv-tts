package io.github.omochice.vvtts.dictionary

import java.io.IOException

/**
 * The words the user registered with their readings, kept in [file].
 *
 * Words that differ only in the case of ASCII letters are one word, because the engine registers
 * every case of a word it applies.
 *
 * Every operation reads or writes the file and may throw [IOException].
 */
class UserDictionary(private val file: UserDictionaryFile) {
  /** Returns the entries ordered by word with ASCII case ignored. */
  fun entries(): List<DictionaryEntry> = DictionaryFormat.decode(file.read())

  /** Stores [entry], replacing the entry of the same word if there is one. */
  fun add(entry: DictionaryEntry) {
    store(entries().filter { it.key != entry.key } + entry)
  }

  /** Removes the entry of [word], if there is one. */
  fun remove(word: String) {
    val key = word.lowercaseAscii()
    store(entries().filter { it.key != key })
  }

  private fun store(entries: List<DictionaryEntry>) {
    file.write(DictionaryFormat.encode(entries))
  }
}
