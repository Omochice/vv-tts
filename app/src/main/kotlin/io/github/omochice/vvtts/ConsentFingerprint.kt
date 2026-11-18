package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.RemoteFile

/**
 * Identifies the terms the user agrees to, so that consent recorded for other terms is told apart.
 *
 * @param terms the voice model terms files, in any order.
 * @param onnxRuntimeReleaseApiUrl the release whose notes carry the ONNX Runtime terms.
 */
fun consentFingerprint(terms: List<RemoteFile>, onnxRuntimeReleaseApiUrl: String): String =
    (terms.map { it.sha256 }.sorted() + onnxRuntimeReleaseApiUrl).joinToString("\n")
