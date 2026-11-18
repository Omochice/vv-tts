package io.github.omochice.vvtts.assets

/**
 * Returns the ONNX Runtime terms published in the Markdown [releaseNotes] of its release, with LF
 * line endings and without surrounding blank space, or null when the notes carry no terms.
 */
fun onnxRuntimeTermsIn(releaseNotes: String): String? =
    TERMS_BLOCK.find(releaseNotes.replace("\r\n", "\n"))?.groupValues?.get(1)?.trim()

private val TERMS_BLOCK =
    Regex(
        """<pre data-voicevox-onnxruntime-terms>\s*<code>(.*?)</code>\s*</pre>""",
        RegexOption.DOT_MATCHES_ALL,
    )
