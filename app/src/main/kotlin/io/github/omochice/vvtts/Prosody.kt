package io.github.omochice.vvtts

/**
 * How fast and how high speech is voiced, in VOICEVOX's own scales.
 *
 * @property speedScale speaking speed, where 1.0 is normal and 2.0 is twice as fast.
 * @property pitchScale pitch offset, where 0.0 is normal and positive values are higher.
 */
data class Prosody(val speedScale: Double, val pitchScale: Double) {
  companion object {
    /** The prosody for an Android speech request, whose [rate] and [pitch] use 100 as normal. */
    fun fromAndroid(rate: Int, pitch: Int): Prosody =
        Prosody(speedScale = rate / 100.0, pitchScale = (pitch - 100) / 100.0 * 0.15)
  }
}
