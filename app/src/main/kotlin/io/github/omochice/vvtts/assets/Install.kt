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
