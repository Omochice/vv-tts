package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.Terms
import io.github.omochice.vvtts.assets.sha256Hex

/**
 * Identifies the text of [terms] the user agrees to, so that consent to other text is told apart.
 */
fun consentFingerprint(terms: Terms): String {
  val shown = terms.voiceModelTerms.flatMap { listOf(it.title, it.text) } + terms.onnxRuntimeTerms
  // Each part is digested on its own rather than all of them joined, so that text moving across a
  // boundary between parts cannot leave the fingerprint unchanged.
  return shown.joinToString("\n") { sha256Hex(it.toByteArray(Charsets.UTF_8)) }
}
