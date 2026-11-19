package io.github.omochice.vvtts.assets

import java.io.File
import java.security.MessageDigest

/**
 * Returns the SHA-256 digest of [file] as 64 lowercase hex characters.
 *
 * @throws java.io.IOException if [file] cannot be read.
 */
fun sha256Hex(file: File): String {
  val digest = MessageDigest.getInstance("SHA-256")
  file.forEachBlock(64 * 1024) { buffer, read -> digest.update(buffer, 0, read) }
  return digest.digest().toHex()
}

/** Returns the SHA-256 digest of [bytes] as 64 lowercase hex characters. */
fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

private fun ByteArray.toHex(): String = toHexString()
