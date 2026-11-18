package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.Terms
import io.github.omochice.vvtts.assets.TermsDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConsentFingerprintTest {
  private val readme = TermsDocument("README.txt", "readme")
  private val termsFile = TermsDocument("TERMS.txt", "terms")
  private val terms = Terms(listOf(readme, termsFile), "onnx runtime")

  @Test
  fun `the same terms give the same fingerprint`() {
    assertEquals(
        consentFingerprint(terms),
        consentFingerprint(Terms(listOf(readme, termsFile), "onnx runtime")),
    )
  }

  @Test
  fun `a voice model document with other text gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(terms),
        consentFingerprint(
            Terms(listOf(readme, termsFile.copy(text = "terms v2")), "onnx runtime")
        ),
    )
  }

  @Test
  fun `a voice model document with another title gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(terms),
        consentFingerprint(
            Terms(listOf(readme, termsFile.copy(title = "LICENSE.txt")), "onnx runtime")
        ),
    )
  }

  @Test
  fun `an added voice model document gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(Terms(listOf(termsFile), "onnx runtime")),
        consentFingerprint(terms),
    )
  }

  @Test
  fun `text moved from one document to the next gives another fingerprint`() {
    assertNotEquals(
        consentFingerprint(
            Terms(listOf(TermsDocument("a", "xy"), TermsDocument("b", "z")), "onnx runtime")
        ),
        consentFingerprint(
            Terms(listOf(TermsDocument("a", "x"), TermsDocument("b", "yz")), "onnx runtime")
        ),
    )
  }

  @Test
  fun `other ONNX Runtime terms give another fingerprint`() {
    assertNotEquals(
        consentFingerprint(terms),
        consentFingerprint(Terms(listOf(readme, termsFile), "onnx runtime v2")),
    )
  }
}
