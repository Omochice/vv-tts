package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.RemoteFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConsentFingerprintTest {
  private val termsFile = terms("TERMS.txt", "a".repeat(64))
  private val readme = terms("README.txt", "b".repeat(64))
  private val release = "https://api.github.com/repos/VOICEVOX/onnxruntime-builder/releases/tags/1"

  private fun terms(fileName: String, sha256: String) =
      RemoteFile(fileName, "https://example.invalid/$fileName", 10, sha256)

  @Test
  fun `the same terms give the same fingerprint`() {
    assertEquals(
        consentFingerprint(listOf(termsFile, readme), release),
        consentFingerprint(listOf(termsFile, readme), release),
    )
  }

  @Test
  fun `the order of the terms files does not matter`() {
    assertEquals(
        consentFingerprint(listOf(termsFile, readme), release),
        consentFingerprint(listOf(readme, termsFile), release),
    )
  }

  @Test
  fun `a terms file with other content gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(listOf(termsFile, readme), release),
        consentFingerprint(listOf(termsFile, terms("README.txt", "c".repeat(64))), release),
    )
  }

  @Test
  fun `an added terms file gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(listOf(termsFile), release),
        consentFingerprint(listOf(termsFile, readme), release),
    )
  }

  @Test
  fun `another ONNX Runtime release gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(listOf(termsFile, readme), release),
        consentFingerprint(listOf(termsFile, readme), release.replace("tags/1", "tags/2")),
    )
  }
}
