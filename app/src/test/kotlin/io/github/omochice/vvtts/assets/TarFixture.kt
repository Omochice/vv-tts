package io.github.omochice.vvtts.assets

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/** Builds small tar archives in memory in the GNU layout that the pinned archives use. */
internal class TarFixture {
  private val tar = ByteArrayOutputStream()

  fun directory(name: String) = entry(name, '5', ByteArray(0))

  fun file(name: String, content: ByteArray) = entry(name, '0', content)

  fun file(name: String, content: String) = file(name, content.toByteArray())

  /**
   * Appends an entry whose header can carry one specific defect.
   *
   * @param magic the bytes at the format magic field, GNU tar unless overridden.
   * @param sizeField the raw size field, the octal size of [content] unless overridden.
   * @param corruptChecksum whether the stored header checksum is off by one.
   */
  fun entry(
      name: String,
      typeflag: Char,
      content: ByteArray,
      magic: String = "ustar  \u0000",
      sizeField: String = "%011o".format(content.size) + "\u0000",
      corruptChecksum: Boolean = false,
  ): TarFixture {
    val header = ByteArray(BLOCK)
    put(header, 0, name)
    put(header, 100, "0000644\u0000")
    put(header, 108, "0001750\u0000")
    put(header, 116, "0001750\u0000")
    put(header, 124, sizeField)
    put(header, 136, "00000000000\u0000")
    put(header, 148, "        ")
    header[156] = typeflag.code.toByte()
    put(header, 257, magic)
    val sum = header.sumOf { it.toInt() and 0xff } + if (corruptChecksum) 1 else 0
    put(header, 148, "%06o".format(sum) + "\u0000 ")
    tar.write(header)
    tar.write(content)
    tar.write(ByteArray((BLOCK - content.size % BLOCK) % BLOCK))
    return this
  }

  /** The archive as a tar stream ending with the two zero blocks that mark its end. */
  fun tarBytes(): ByteArray = tar.toByteArray() + ByteArray(2 * BLOCK)

  /** The archive gzipped, optionally without its end-of-archive blocks. */
  fun gzip(endMarker: Boolean = true): ByteArray =
      gzip(if (endMarker) tarBytes() else tar.toByteArray())

  private fun put(header: ByteArray, offset: Int, value: String) {
    value.toByteArray().copyInto(header, offset)
  }

  companion object {
    const val BLOCK = 512

    fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream()
            .also { out -> GZIPOutputStream(out).use { it.write(bytes) } }
            .toByteArray()
  }
}
