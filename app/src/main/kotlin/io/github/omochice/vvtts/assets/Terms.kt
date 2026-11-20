package io.github.omochice.vvtts.assets

/** One terms document, whose [text] is shown verbatim under [title]. */
data class TermsDocument(val title: String, val text: String)

/**
 * The terms the user agrees to before the download, as bundled with the app.
 *
 * @property voiceModelTerms the documents published with the voice models, titled by file name.
 * @property onnxRuntimeTerms the terms shipped in the ONNX Runtime archive.
 */
class Terms(val voiceModelTerms: List<TermsDocument>, val onnxRuntimeTerms: String) {
  companion object {
    /**
     * Reads the bundled documents [catalog] lists as terms, and the ONNX Runtime terms.
     *
     * @throws IllegalStateException if a document is not bundled.
     */
    fun load(catalog: AssetCatalog): Terms =
        Terms(
            voiceModelTerms =
                catalog.terms.map { TermsDocument(it.fileName, resource(it.fileName)) },
            // Trimmed, because the file ends with a line break that the dialog never showed.
            onnxRuntimeTerms = resource(ONNX_RUNTIME_TERMS).trim(),
        )

    private fun resource(fileName: String): String =
        checkNotNull(Terms::class.java.getResourceAsStream("terms/$fileName")) {
              "terms/$fileName is not bundled"
            }
            .use { it.readBytes().toString(Charsets.UTF_8) }

    private const val ONNX_RUNTIME_TERMS = "ONNXRUNTIME_TERMS.txt"
  }
}
