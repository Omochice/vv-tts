package io.github.omochice.vvtts.assets

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class Sha256Test {
  @get:Rule val tmp = TemporaryFolder()

  @Test
  fun `the digest of a file is its SHA-256 in lowercase hex`() {
    val file = tmp.newFile().apply { writeText("abc") }

    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        sha256Hex(file),
    )
  }

  @Test
  fun `the digest of bytes is their SHA-256 in lowercase hex`() {
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        sha256Hex("abc".toByteArray()),
    )
  }

  @Test
  fun `a file spanning many read buffers digests the same as its whole content`() {
    val content = ByteArray(300_001) { (it * 31).toByte() }
    val file = tmp.newFile().apply { writeBytes(content) }

    val expected =
        MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
    assertEquals(expected, sha256Hex(file))
  }
}
