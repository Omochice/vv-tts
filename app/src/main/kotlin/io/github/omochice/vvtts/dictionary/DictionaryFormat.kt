package io.github.omochice.vvtts.dictionary

private const val SEPARATOR = '\t'

/**
 * Converts entries to and from the text of the user dictionary file, one `word<TAB>reading` line
 * per entry.
 */
object DictionaryFormat {
  /** Returns the text that stores [entries] in their order. */
  fun encode(entries: List<DictionaryEntry>): String =
      entries.joinToString("") { "${it.word}$SEPARATOR${it.reading}\n" }

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
  if (fields.size != 2) return null
  return (DictionaryEntry.of(fields[0], fields[1]) as? EntryCheck.Valid)?.entry
}
