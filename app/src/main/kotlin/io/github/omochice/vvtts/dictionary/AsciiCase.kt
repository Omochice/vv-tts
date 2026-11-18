package io.github.omochice.vvtts.dictionary

// Only ASCII letters are mapped, because VOICEVOX matching was measured with ASCII words alone, and
// full case mapping changes lengths, as ß becomes SS.
internal fun String.lowercaseAscii(): String =
    map { if (it in 'A'..'Z') it + ('a' - 'A') else it }.joinToString("")

internal fun String.uppercaseAscii(): String =
    map { if (it in 'a'..'z') it - ('a' - 'A') else it }.joinToString("")

/** The key under which [DictionaryEntry.word]s that differ only in ASCII case are the same word. */
internal val DictionaryEntry.key: String
  get() = word.lowercaseAscii()
