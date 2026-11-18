package io.github.omochice.vvtts

/** Speaks a sequence of text chunks into a [PcmSink], one chunk at a time. */
class SynthesisLoop(private val engine: SpeechEngine) {
  /**
   * Synthesizes [chunks] in order, every one voiced as [prosody], and writes their audio to [sink].
   *
   * Returns early, without synthesizing another chunk or writing another slice, once
   * [isStopRequested] answers true or the sink refuses a write.
   */
  fun run(
      chunks: List<String>,
      prosody: Prosody,
      sink: PcmSink,
      isStopRequested: () -> Boolean,
  ) {
    for (chunk in chunks) {
      if (isStopRequested()) return
      if (!writeInSlices(engine.synthesize(chunk, prosody), sink, isStopRequested)) return
    }
  }

  private fun writeInSlices(
      pcm: ByteArray,
      sink: PcmSink,
      isStopRequested: () -> Boolean,
  ): Boolean {
    var offset = 0
    while (offset < pcm.size) {
      if (isStopRequested()) return false
      val length = minOf(sink.maxWriteBytes, pcm.size - offset)
      if (!sink.write(pcm, offset, length)) return false
      offset += length
    }
    return true
  }
}
