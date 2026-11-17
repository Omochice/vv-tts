package io.github.omochice.vvtts

/** Receives synthesized PCM in the format [SpeechEngine.synthesize] produces. */
interface PcmSink {
  /** The largest number of bytes a single [write] accepts; always positive. */
  val maxWriteBytes: Int

  /**
   * Takes [length] bytes of [pcm] starting at [offset], where `0 < length <= maxWriteBytes`.
   *
   * @return false when the sink refuses further audio, so the caller must stop writing.
   */
  fun write(pcm: ByteArray, offset: Int, length: Int): Boolean
}
