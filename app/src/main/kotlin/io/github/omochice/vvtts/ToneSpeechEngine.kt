package io.github.omochice.vvtts

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TONE_HZ = 440.0
private const val TONE_MILLIS = 150
private const val SILENCE_MILLIS = 100
private const val AMPLITUDE = 0.3 * Short.MAX_VALUE

/**
 * A stand-in [SpeechEngine] that answers every chunk with the same short beep and pause.
 *
 * One beep per chunk lets a listener count how the text was split.
 */
class ToneSpeechEngine : SpeechEngine {
  private val pcm: ByteArray = beepThenSilence()

  override fun synthesize(chunk: String): ByteArray = pcm.copyOf()

  private fun beepThenSilence(): ByteArray {
    val toneSamples = SpeechEngine.SAMPLE_RATE_HZ * TONE_MILLIS / 1000
    val silenceSamples = SpeechEngine.SAMPLE_RATE_HZ * SILENCE_MILLIS / 1000
    val bytes = ByteArray((toneSamples + silenceSamples) * 2)
    for (i in 0 until toneSamples) {
      val sample =
          (AMPLITUDE * sin(2 * PI * TONE_HZ * i / SpeechEngine.SAMPLE_RATE_HZ)).roundToInt()
      bytes[2 * i] = sample.toByte()
      bytes[2 * i + 1] = (sample shr 8).toByte()
    }
    return bytes
  }
}
