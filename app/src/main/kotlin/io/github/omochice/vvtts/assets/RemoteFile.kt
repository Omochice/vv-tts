package io.github.omochice.vvtts.assets

import java.io.File

/**
 * A file published at [url] whose exact content is pinned by [sizeBytes] and [sha256].
 *
 * @property fileName the name the file is published under, which is also the last segment of [url].
 * @property sizeBytes the exact length in bytes.
 * @property sha256 the SHA-256 digest as 64 lowercase hex characters.
 */
data class RemoteFile(
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
) {
  /**
   * Whether [file] holds exactly the pinned content.
   *
   * @throws java.io.IOException if [file] exists but cannot be read.
   */
  fun isContentOf(file: File): Boolean = file.length() == sizeBytes && sha256Hex(file) == sha256
}
