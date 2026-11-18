package io.github.omochice.vvtts.assets

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Makes [source] read-only and atomically renames it to [target], so [target] never exists in a
 * writable or half-written state.
 *
 * [source] and [target] must be on the same file system.
 *
 * @throws IOException if [source] cannot be made read-only or moved.
 */
fun installReadOnly(source: File, target: File) {
  if (!source.setReadOnly()) throw IOException("cannot make $source read-only")
  Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
}

/**
 * Extracts [archive] as [extractTarGz] does and atomically renames the result to [target], so
 * [target] never exists half-extracted or holding a writable file.
 *
 * Whatever [target] held before is replaced.
 *
 * The files are extracted into a sibling of [target] named with a `.tmp` suffix, and one left there
 * by an interrupted call is discarded first.
 *
 * @throws MalformedArchiveException if [archive] is not of the shape [extractTarGz] accepts.
 * @throws IOException if reading [archive] or writing next to [target] fails.
 */
fun installExtracted(archive: File, target: File) {
  val staging = File(target.parentFile, target.name + ".tmp")
  deleteTree(staging)
  try {
    if (!staging.mkdirs()) throw IOException("cannot create $staging")
    archive.inputStream().use { extractTarGz(it, staging) }
    staging
        .walk()
        .filter { it.isFile }
        .forEach { if (!it.setReadOnly()) throw IOException("cannot make $it read-only") }
    deleteTree(target)
    Files.move(staging.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
  } catch (e: IOException) {
    staging.deleteRecursively()
    throw e
  }
}

private fun deleteTree(file: File) {
  if (file.exists() && !file.deleteRecursively()) throw IOException("cannot delete $file")
}
