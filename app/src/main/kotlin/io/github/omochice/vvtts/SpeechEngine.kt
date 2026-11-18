package io.github.omochice.vvtts

/** Turns one chunk of text into speech audio. */
interface SpeechEngine {
  /**
   * Returns the speech for [chunk] as signed 16-bit little-endian mono PCM at [SAMPLE_RATE_HZ].
   *
   * The result is empty when [chunk] has nothing to pronounce, such as a run of punctuation.
   */
  fun synthesize(chunk: String): ByteArray

  companion object {
    /** Samples per second of every [synthesize] result, matching VOICEVOX's output. */
    const val SAMPLE_RATE_HZ = 24_000
  }
}
