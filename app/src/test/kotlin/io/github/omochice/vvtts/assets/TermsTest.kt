package io.github.omochice.vvtts.assets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TermsTest {
  private val catalog = AssetCatalog.load()
  private val terms = Terms.load(catalog)

  @Test
  fun `the voice model terms are the documents the catalog lists, titled by file name`() {
    assertEquals(listOf("README.txt", "TERMS.txt"), terms.voiceModelTerms.map { it.title })
  }

  // The documents come from the releases pinned in nvfetcher.toml, which the build does not check
  // against assets.tsv, so a release bumped in only one of them fails here.
  // The ONNX Runtime terms are not pinned here, because the build already refuses an ONNX Runtime
  // archive whose hash differs from its row in assets.tsv.
  // To follow a new release, bump its version in both files and run `nix run .#nvfetcher`.
  @Test
  fun `each bundled voice model document is the published file the catalog pins`() {
    val digests = terms.voiceModelTerms.map { sha256Hex(it.text.toByteArray(Charsets.UTF_8)) }

    assertEquals(catalog.terms.map { it.sha256 }, digests)
  }

  @Test
  fun `the ONNX Runtime terms are shown without the line break that ends the bundled file`() {
    assertTrue(terms.onnxRuntimeTerms.startsWith("# VOICEVOX ONNX Runtime 利用規約\n"))
    assertTrue(terms.onnxRuntimeTerms.endsWith("クレジット表記が必要です。"))
  }

  @Test
  fun `a catalog listing a terms document that is not bundled is rejected`() {
    val unbundled = RemoteFile("LICENSE.txt", "https://github.com/x/LICENSE.txt", 1, "0".repeat(64))
    val withUnbundled =
        AssetCatalog(
            dictionaryArchive = catalog.dictionaryArchive,
            terms = catalog.terms + unbundled,
            voiceModels = catalog.voiceModels,
            onnxRuntimeArchives = catalog.onnxRuntimeArchives,
        )

    assertFailsWith<IllegalStateException> { Terms.load(withUnbundled) }
  }
}
