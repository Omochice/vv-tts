package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkLengthTest {
  private fun maxAt(speedScale: Double): Int = maxChunkLength(Prosody(speedScale, pitchScale = 0.0))

  @Test
  fun `normal speed allows 80 code units`() {
    assertEquals(80, maxAt(1.0))
  }

  @Test
  fun `a slower speed allows proportionally fewer code units, rounded down`() {
    assertEquals(49, maxAt(0.62))
  }

  @Test
  fun `a faster speed allows proportionally more code units`() {
    assertEquals(160, maxAt(2.0))
  }

  @Test
  fun `a very slow speed is held at the floor of 20 code units`() {
    assertEquals(20, maxAt(0.01))
  }

  private fun chunksAt(speedScale: Double, text: String): List<String> =
      speechChunks(text, Prosody(speedScale, pitchScale = 0.0))

  @Test
  fun `speech chunks are split into sentences`() {
    assertEquals(listOf("今日は晴れ。", "明日は雨。"), chunksAt(1.0, "今日は晴れ。明日は雨。"))
  }

  @Test
  fun `ruby is replaced by its reading before the text is split into sentences`() {
    assertEquals(listOf("すごいと言った。"), chunksAt(1.0, "｜すごい！《すごい》と言った。"))
  }

  @Test
  fun `a sentence longer than the cap for the speed is cut at the cap`() {
    val sentence = "あ".repeat(60) + "。"
    assertEquals(listOf(sentence), chunksAt(1.0, sentence))
    assertEquals(listOf("あ".repeat(40), "あ".repeat(20) + "。"), chunksAt(0.5, sentence))
  }
}
