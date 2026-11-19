package io.github.omochice.vvtts.dictionary

internal fun validEntry(word: String, reading: String, accent: Int = 0): DictionaryEntry =
    (DictionaryEntry.of(word, reading, accent) as EntryCheck.Valid).entry
