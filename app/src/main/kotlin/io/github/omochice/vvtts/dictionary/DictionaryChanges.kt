package io.github.omochice.vvtts.dictionary

import java.io.IOException

/**
 * Tells the speech engine when the dictionary in [file] differs from what it last applied.
 *
 * The file's text is compared rather than its modification time and length, because a reading
 * replaced by one of the same length within the time resolution of the file system would go
 * unnoticed, and reading a file of a few kilobytes costs little next to a synthesis.
 */
class DictionaryChanges(private val file: UserDictionaryFile) {
  private var lastText: String? = null

  /**
   * Returns the entries when the file changed since the previous call, or on the first call;
   * otherwise null.
   *
   * A change counts as seen once it is returned, so a failure to apply it is not retried on every
   * call.
   *
   * @throws IOException if the file exists but cannot be read; the next call looks again.
   */
  fun changedEntries(): List<DictionaryEntry>? {
    val text = file.read()
    if (text == lastText) return null
    lastText = text
    return DictionaryFormat.decode(text)
  }
}
