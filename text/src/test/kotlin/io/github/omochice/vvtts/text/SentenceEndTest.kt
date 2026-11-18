package io.github.omochice.vvtts.text

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SentenceEndTest {
  @Test
  fun `full stops, exclamation and question marks of both widths and newlines end sentences`() {
    for (char in "。！？!?\n") assertTrue(isSentenceEnd(char), "$char")
  }

  @Test
  fun `commas, periods and closing brackets do not end sentences`() {
    for (char in "、，,.」』）)") assertFalse(isSentenceEnd(char), "$char")
  }
}
