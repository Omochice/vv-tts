package io.github.omochice.vvtts.dictionary

// Only ASCII letters are mapped, because VOICEVOX matching was measured with ASCII words alone,
// and full case mapping changes lengths, as ß becomes SS.
internal fun String.lowercaseAscii(): String =
    String(CharArray(length) { this[it].let { c -> if (c in 'A'..'Z') c + ('a' - 'A') else c } })

internal fun String.uppercaseAscii(): String =
    String(CharArray(length) { this[it].let { c -> if (c in 'a'..'z') c - ('a' - 'A') else c } })

/** The key under which [DictionaryEntry.word]s that differ only in ASCII case are the same word. */
internal val DictionaryEntry.key: String
  get() = word.lowercaseAscii()
