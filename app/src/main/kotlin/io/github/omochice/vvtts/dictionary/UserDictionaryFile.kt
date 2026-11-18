package io.github.omochice.vvtts.dictionary

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The file under [dir] that holds the user dictionary.
 *
 * It sits beside the speech assets rather than inside them, because the assets are replaced as a
 * whole when they are installed again, while the dictionary is the user's own data.
 */
class UserDictionaryFile(private val dir: File) {
  /** Where the file is, `user-dictionary.tsv` directly under the directory. */
  val path = File(dir, FILE_NAME)

  /**
   * Returns the stored text, empty when nothing was stored yet.
   *
   * @throws IOException if the file exists but cannot be read.
   */
  fun read(): String =
      try {
        path.readText(Charsets.UTF_8)
      } catch (_: FileNotFoundException) {
        ""
      }

  /**
   * Replaces the stored text with [text] so that a reader sees either the old or the new text
   * whole.
   *
   * @throws IOException if the text cannot be written.
   */
  fun write(text: String) {
    dir.mkdirs()
    val temp = File(dir, "$FILE_NAME.tmp")
    temp.outputStream().use {
      it.write(text.toByteArray(Charsets.UTF_8))
      it.fd.sync()
    }
    Files.move(
        temp.toPath(),
        path.toPath(),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING,
    )
  }

  private companion object {
    const val FILE_NAME = "user-dictionary.tsv"
  }
}
