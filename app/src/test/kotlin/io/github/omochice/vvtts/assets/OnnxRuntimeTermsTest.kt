package io.github.omochice.vvtts.assets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnnxRuntimeTermsTest {
  @Test
  fun `the terms are the text inside the marked pre and code elements`() {
    val notes =
        """
        |## Downloads
        |
        |<table><tr><td>x</td></tr></table>
        |
        |<pre data-voicevox-onnxruntime-terms><code># VOICEVOX ONNX Runtime 利用規約
        |
        |## 許諾内容
        |
        |1. 商用・非商用問わず利用することができます
        |</code></pre>
        |
        |trailing text
        """
            .trimMargin()

    assertEquals(
        "# VOICEVOX ONNX Runtime 利用規約\n\n## 許諾内容\n\n1. 商用・非商用問わず利用することができます",
        onnxRuntimeTermsIn(notes),
    )
  }

  @Test
  fun `CRLF line endings become LF`() {
    val notes =
        "intro\r\n<pre data-voicevox-onnxruntime-terms>\r\n<code># Terms\r\n\r\nline\r\n</code></pre>\r\n"

    assertEquals("# Terms\n\nline", onnxRuntimeTermsIn(notes))
  }

  @Test
  fun `release notes without the marker have no terms`() {
    assertNull(onnxRuntimeTermsIn("<pre><code># Something else\n</code></pre>"))
  }

  @Test
  fun `a marker without its closing elements gives no terms`() {
    assertNull(onnxRuntimeTermsIn("<pre data-voicevox-onnxruntime-terms><code># Terms\n"))
  }
}
