package io.github.omochice.vvtts.dictionary

/**
 * Returns the words to register with VOICEVOX for [entries]: each entry as stored, then for a word
 * with ASCII letters its lower-case, upper-case and capitalized forms with the same reading.
 *
 * VOICEVOX matches words case-sensitively, so one entry would otherwise cover only the case it was
 * typed in. A word as stored wins over the same form generated from another entry, and each form is
 * registered once.
 */
fun wordsToRegister(entries: List<DictionaryEntry>): List<DictionaryEntry> {
  val variants = entries.flatMap { entry ->
    caseVariantsOf(entry.word).map { DictionaryEntry(it, entry.reading) }
  }
  return (entries + variants).distinctBy { it.word }
}

private fun caseVariantsOf(word: String): List<String> {
  val lower = word.lowercaseAscii()
  return listOf(lower, word.uppercaseAscii(), lower.take(1).uppercaseAscii() + lower.drop(1))
}
