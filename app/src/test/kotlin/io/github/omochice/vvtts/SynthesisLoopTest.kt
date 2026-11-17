package io.github.omochice.vvtts

import io.github.omochice.vvtts.text.SentenceSplitter
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SynthesisLoopTest {
  private class RecordingEngine(private val bytesPerChunk: Int) : SpeechEngine {
    val synthesized = mutableListOf<String>()

    override fun synthesize(chunk: String): ByteArray {
      synthesized += chunk
      return ByteArray(bytesPerChunk) { (synthesized.size * 100 + it).toByte() }
    }
  }

  private class RecordingSink(
      override val maxWriteBytes: Int,
      private val acceptedWrites: Int = Int.MAX_VALUE,
  ) : PcmSink {
    val writes = mutableListOf<ByteArray>()

    override fun write(pcm: ByteArray, offset: Int, length: Int): Boolean {
      writes += pcm.copyOfRange(offset, offset + length)
      return writes.size <= acceptedWrites
    }
  }

  @Test
  fun `each chunk is synthesized once in reading order`() {
    val engine = RecordingEngine(bytesPerChunk = 4)

    SynthesisLoop(engine).run(listOf("一。", "二。", "三。"), RecordingSink(16)) { false }

    assertEquals(listOf("一。", "二。", "三。"), engine.synthesized)
  }

  @Test
  fun `audio larger than the sink limit reaches the sink in order in slices within the limit`() {
    val engine = RecordingEngine(bytesPerChunk = 10)
    val sink = RecordingSink(maxWriteBytes = 4)

    SynthesisLoop(engine).run(listOf("一。", "二。"), sink) { false }

    assertEquals(listOf(4, 4, 2, 4, 4, 2), sink.writes.map { it.size })
    val expected = ByteArray(10) { (100 + it).toByte() } + ByteArray(10) { (200 + it).toByte() }
    assertContentEquals(expected, sink.writes.reduce(ByteArray::plus))
  }

  @Test
  fun `audio within the sink limit is written in a single slice`() {
    val sink = RecordingSink(maxWriteBytes = 8)

    SynthesisLoop(RecordingEngine(bytesPerChunk = 8)).run(listOf("一。"), sink) { false }

    assertEquals(listOf(8), sink.writes.map { it.size })
  }

  @Test
  fun `no chunks means nothing is synthesized or written`() {
    val engine = RecordingEngine(bytesPerChunk = 4)
    val sink = RecordingSink(16)

    SynthesisLoop(engine).run(SentenceSplitter(100).split(" \n　"), sink) { false }

    assertTrue(engine.synthesized.isEmpty())
    assertTrue(sink.writes.isEmpty())
  }

  @Test
  fun `a stop raised before the request starts synthesizes nothing`() {
    val engine = RecordingEngine(bytesPerChunk = 4)
    val sink = RecordingSink(16)

    SynthesisLoop(engine).run(listOf("一。", "二。"), sink) { true }

    assertTrue(engine.synthesized.isEmpty())
    assertTrue(sink.writes.isEmpty())
  }

  @Test
  fun `a stop raised while a chunk is written prevents the next slice and the next chunk`() {
    val engine = RecordingEngine(bytesPerChunk = 10)
    val sink = RecordingSink(maxWriteBytes = 4)

    SynthesisLoop(engine).run(listOf("一。", "二。"), sink) { sink.writes.size >= 2 }

    assertEquals(2, sink.writes.size)
    assertEquals(listOf("一。"), engine.synthesized)
  }

  @Test
  fun `a stop raised after a chunk is fully written prevents synthesizing the next chunk`() {
    val engine = RecordingEngine(bytesPerChunk = 4)
    val sink = RecordingSink(maxWriteBytes = 4)

    SynthesisLoop(engine).run(listOf("一。", "二。"), sink) { sink.writes.size >= 1 }

    assertEquals(listOf("一。"), engine.synthesized)
  }

  @Test
  fun `a refused slice ends the request without further slices or chunks`() {
    val engine = RecordingEngine(bytesPerChunk = 10)
    val sink = RecordingSink(maxWriteBytes = 4, acceptedWrites = 1)

    SynthesisLoop(engine).run(listOf("一。", "二。"), sink) { false }

    assertEquals(2, sink.writes.size)
    assertEquals(listOf("一。"), engine.synthesized)
  }
}
