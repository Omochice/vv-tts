package io.github.omochice.vvtts.dictionary

private const val SEPARATOR = '\t'
private val DIGITS = Regex("[0-9]+")

/**
 * Converts entries to and from the text of the user dictionary file, one
 * `word<TAB>reading<TAB>accent` line per entry.
 *
 * A `word<TAB>reading` line, as written before accents were stored, reads as flat.
 */
object DictionaryFormat {
  /** Returns the text that stores [entries] in their order. */
  fun encode(entries: List<DictionaryEntry>): String =
      entries.joinToString("") {
        "${it.word}$SEPARATOR${it.reading}$SEPARATOR${it.accent}\n"
      }

  /**
   * Returns the entries stored in [text], ordered by word with ASCII case ignored.
   *
   * A line that is not one valid entry is dropped, because the file may have been edited by hand
   * and an entry that reaches VOICEVOX unchecked can crash the process. Of lines whose words differ
   * only in ASCII case, the last one is kept.
   */
  fun decode(text: String): List<DictionaryEntry> =
      text
          .lineSequence()
          .mapNotNull(::entryOf)
          .associateBy { it.key }
          .values
          .sortedWith(compareBy<DictionaryEntry> { it.key }.thenBy { it.word })
}

private fun entryOf(line: String): DictionaryEntry? {
  val fields = line.split(SEPARATOR)
  val accent =
      when (fields.size) {
        2 -> 0
        3 -> accentOf(fields[2].trimEnd('\r')) ?: return null
        else -> return null
      }
  return (DictionaryEntry.of(fields[0], fields[1], accent) as? EntryCheck.Valid)?.entry
}

// toIntOrNull alone would accept a sign, such as +3, which no writer of this file produces.
private fun accentOf(field: String): Int? = if (DIGITS.matches(field)) field.toIntOrNull() else null
