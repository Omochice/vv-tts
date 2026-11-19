package io.github.omochice.vvtts.assets

import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

/**
 * Extracts a gzipped GNU tar archive whose entries all sit under one top-level directory, placing
 * that directory's contents directly under [destination].
 *
 * Only regular files and directories are understood.
 *
 * Files already present under [destination] are never overwritten, so extract into an empty
 * directory.
 *
 * On failure, entries extracted so far are left in place for the caller to discard.
 *
 * [input] is closed on return, whether or not the extraction succeeded.
 *
 * @throws MalformedArchiveException if the input is not gzip, or the archive is truncated, has an
 *   unknown entry type, header format or checksum, a path leaving [destination], an entry outside
 *   the single top-level directory, or data after its end-of-archive marker.
 * @throws java.nio.file.FileAlreadyExistsException if an entry's file already exists.
 * @throws IOException if reading [input] or writing under [destination] fails.
 */
fun extractTarGz(input: InputStream, destination: File) {
  try {
    // GZIPInputStream can throw from its constructor, before there is a wrapper to close.
    input.use {
      // DataInputStream rather than InputStream.readNBytes, which Android only has from API 33.
       DataInputStream(GZIPInputStream(input, 64 * 1024)).use { tar ->
        extractEntries(tar, destination)
        readToEnd(tar)
      }
    }
  } catch (e: EOFException) {
    throw MalformedArchiveException("archive ends early", e)
  } catch (e: ZipException) {
    throw MalformedArchiveException("archive is not valid gzip", e)
  }
}

/** Reads past the end-of-archive marker so that gzip checks its trailing CRC and length. */
private fun readToEnd(tar: DataInputStream) {
  val buffer = ByteArray(BLOCK)
  while (true) {
    val read = tar.read(buffer)
    if (read < 0) return
    if ((0 until read).any { buffer[it] != 0.toByte() }) {
      throw MalformedArchiveException("data follows the end-of-archive marker")
    }
  }
}

private fun extractEntries(tar: DataInputStream, destination: File) {
  val block = ByteArray(BLOCK)
  var topLevel: String? = null
  while (true) {
    tar.readFully(block)
    val header = TarHeader.parse(block) ?: return
    val components = pathComponents(header.name)
    val top = components.first()
    val inside = components.drop(1)
    if (topLevel == null) topLevel = top
    if (top != topLevel || (inside.isEmpty() && !header.isDirectory)) {
      throw MalformedArchiveException("${header.name} is outside the top-level directory $topLevel")
    }
    if (inside.isEmpty()) continue
    val target = File(destination, inside.joinToString(File.separator))
    if (header.isDirectory) {
      target.mkdirs()
    } else {
      Files.newOutputStream(target.toPath(), StandardOpenOption.CREATE_NEW).use {
        copyExactly(tar, it, header.size)
      }
    }
    tar.skipFully(paddingAfter(header.size))
  }
}

private fun paddingAfter(size: Long): Long = (BLOCK - size % BLOCK) % BLOCK

private class TarHeader(val name: String, val size: Long, val isDirectory: Boolean) {
  companion object {
    /** Returns null for the zero block that ends the archive. */
    fun parse(block: ByteArray): TarHeader? {
      if (block.all { it == 0.toByte() }) return null
      val name = String(block, NAME_OFFSET, NAME_LENGTH).trimEnd('\u0000')
      if (octal(block, CHECKSUM_OFFSET, CHECKSUM_LENGTH) != checksum(block)) {
        throw MalformedArchiveException("header checksum mismatch for $name")
      }
      // POSIX ustar is refused rather than also accepted, because its prefix field would have to be
      // joined to the name, while both pinned archives are GNU tar, where that field means
      // something else.
      if (String(block, MAGIC_OFFSET, GNU_MAGIC.length, Charsets.US_ASCII) != GNU_MAGIC) {
        throw MalformedArchiveException("$name is not in GNU tar format")
      }
      val isDirectory =
          when (block[TYPEFLAG_OFFSET].toInt().toChar()) {
            '0' -> false
            '5' -> true
            else ->
                throw MalformedArchiveException(
                    "unsupported entry type '${block[TYPEFLAG_OFFSET].toInt().toChar()}' for $name"
                )
          }
      return TarHeader(
          name = name,
          size = octal(block, SIZE_OFFSET, SIZE_LENGTH),
          isDirectory = isDirectory,
      )
    }

    private fun checksum(block: ByteArray): Long =
        block.indices.sumOf {
          if (it in CHECKSUM_OFFSET until CHECKSUM_OFFSET + CHECKSUM_LENGTH) {
            CHECKSUM_PLACEHOLDER.code.toLong()
          } else {
            block[it].toUByte().toLong()
          }
        }

    private fun octal(block: ByteArray, offset: Int, length: Int): Long {
      val digits =
          String(block, offset, length, Charsets.US_ASCII).trimStart(' ').takeWhile {
            it != '\u0000' && it != ' '
          }
      if (digits.isEmpty() || digits.any { it !in '0'..'7' }) {
        throw MalformedArchiveException("unreadable numeric header field at offset $offset")
      }
      return digits.toLong(radix = 8)
    }
  }
}

/**
 * Rejects suspicious components rather than comparing canonical paths, because canonicalization
 * follows symbolic links already present in the destination and so depends on more than the
 * archive.
 */
private fun pathComponents(name: String): List<String> {
  val components = name.removeSuffix("/").split('/')
  if (components.any { it.isEmpty() || it == "." || it == ".." }) {
    throw MalformedArchiveException("entry path $name does not stay inside the destination")
  }
  return components
}

private fun copyExactly(input: DataInputStream, output: OutputStream, size: Long) {
  val buffer = ByteArray(64 * 1024)
  var remaining = size
  while (remaining > 0) {
    val chunk = minOf(remaining, buffer.size.toLong()).toInt()
    input.readFully(buffer, 0, chunk)
    output.write(buffer, 0, chunk)
    remaining -= chunk
  }
}

private fun DataInputStream.skipFully(count: Long) {
  readFully(ByteArray(count.toInt()))
}

private const val BLOCK = 512
private const val GNU_MAGIC = "ustar  \u0000"
private const val NAME_OFFSET = 0
private const val NAME_LENGTH = 100
private const val SIZE_OFFSET = 124
private const val SIZE_LENGTH = 12
private const val CHECKSUM_OFFSET = 148
private const val CHECKSUM_LENGTH = 8
private const val TYPEFLAG_OFFSET = 156
private const val MAGIC_OFFSET = 257

private const val CHECKSUM_PLACEHOLDER = ' '
