package io.github.omochice.vvtts

// Provisional: a reader app's speech rate 10 took the engine to 1.48 GB PSS on a Pixel 9a,
// and 0.5 is the lowest speed VOICEVOX's own editor offers.
// Rejecting a slower rate instead would leave the reader app silent.
private const val PROVISIONAL_MIN_SPEED_SCALE = 0.5

/**
 * How fast and how high speech is voiced, in VOICEVOX's own scales.
 *
 * @property speedScale speaking speed, where 1.0 is normal and 2.0 is twice as fast.
 * @property pitchScale pitch offset, where 0.0 is normal and positive values are higher.
 */
data class Prosody(val speedScale: Double, val pitchScale: Double) {
  companion object {
    /**
     * The prosody for an Android speech request, whose [rate] and [pitch] use 100 as normal.
     *
     * A [rate] below 50 is spoken at half the normal speed.
     */
    fun fromAndroid(rate: Int, pitch: Int): Prosody =
        Prosody(
            speedScale = (rate / 100.0).coerceAtLeast(PROVISIONAL_MIN_SPEED_SCALE),
            pitchScale = (pitch - 100) / 100.0 * 0.15,
        )
  }
}
