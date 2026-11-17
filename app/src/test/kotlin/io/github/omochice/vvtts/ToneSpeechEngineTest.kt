package io.github.omochice.vvtts

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneSpeechEngineTest {
  private val engine = ToneSpeechEngine()

  private fun samplesOf(pcm: ByteArray): ShortArray {
    val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    return ShortArray(buffer.remaining()).also { buffer.get(it) }
  }

  private fun samplesFor(millis: Int) = SpeechEngine.SAMPLE_RATE_HZ * millis / 1000

  @Test
  fun `a chunk becomes 150 ms of tone followed by 100 ms of silence in 16-bit samples`() {
    val pcm = engine.synthesize("吾輩は猫である。")

    assertEquals((samplesFor(150) + samplesFor(100)) * 2, pcm.size)
  }

  @Test
  fun `the trailing 100 ms is silent`() {
    val samples = samplesOf(engine.synthesize("吾輩は猫である。"))

    assertTrue(samples.takeLast(samplesFor(100)).all { it == 0.toShort() })
  }

  @Test
  fun `the tone decoded as little-endian peaks near its amplitude`() {
    val tone = samplesOf(engine.synthesize("吾輩は猫である。")).take(samplesFor(150))

    val peak = tone.maxOf { abs(it.toInt()) }
    assertTrue(peak in 9000..10000, "peak was $peak")
  }

  @Test
  fun `the tone oscillates at 440 Hz`() {
    val tone = samplesOf(engine.synthesize("吾輩は猫である。")).take(samplesFor(150))

    val signChanges = tone.zipWithNext().count { (a, b) -> (a < 0) != (b < 0) }
    val expected = 2 * 440 * 150 / 1000
    assertTrue(abs(signChanges - expected) <= 2, "sign changes were $signChanges")
  }

  @Test
  fun `every chunk gets the same beep regardless of its text`() {
    assertContentEquals(engine.synthesize("あ"), engine.synthesize("名前はまだ無い。"))
  }
}
