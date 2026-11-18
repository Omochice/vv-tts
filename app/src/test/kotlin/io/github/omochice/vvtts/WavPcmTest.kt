package io.github.omochice.vvtts

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class WavPcmTest {
  private class Chunk(val id: String, val body: ByteArray, val declaredSize: Int = body.size)

  private fun le16(value: Int) =
      ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()

  private fun le32(value: Int) =
      ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

  private fun fmt(
      formatTag: Int = 1,
      channels: Int = 1,
      sampleRate: Int = SpeechEngine.SAMPLE_RATE_HZ,
      bitsPerSample: Int = 16,
      extra: ByteArray = ByteArray(0),
  ): Chunk {
    val blockAlign = channels * bitsPerSample / 8
    return Chunk(
        "fmt ",
        le16(formatTag) +
            le16(channels) +
            le32(sampleRate) +
            le32(sampleRate * blockAlign) +
            le16(blockAlign) +
            le16(bitsPerSample) +
            extra,
    )
  }

  private fun wav(vararg chunks: Chunk, riff: String = "RIFF", wave: String = "WAVE"): ByteArray {
    val body = ByteArrayOutputStream()
    body.write(wave.toByteArray(Charsets.US_ASCII))
    for (chunk in chunks) {
      body.write(chunk.id.toByteArray(Charsets.US_ASCII))
      body.write(le32(chunk.declaredSize))
      body.write(chunk.body)
      if (chunk.body.size % 2 == 1) body.write(0)
    }
    return riff.toByteArray(Charsets.US_ASCII) + le32(body.size()) + body.toByteArray()
  }

  private val samples = ByteArray(8) { (it + 1).toByte() }

  @Test
  fun `the payload of the data chunk is returned`() {
    assertContentEquals(samples, pcmFromWav(wav(fmt(), Chunk("data", samples))))
  }

  @Test
  fun `an empty data chunk gives empty audio`() {
    assertContentEquals(ByteArray(0), pcmFromWav(wav(fmt(), Chunk("data", ByteArray(0)))))
  }

  @Test
  fun `unknown chunks before and after fmt are skipped`() {
    val wav =
        wav(Chunk("LIST", ByteArray(6)), fmt(), Chunk("fact", ByteArray(4)), Chunk("data", samples))

    assertContentEquals(samples, pcmFromWav(wav))
  }

  @Test
  fun `an odd-sized unknown chunk is followed by its pad byte`() {
    val wav = wav(fmt(), Chunk("junk", ByteArray(3) { 9 }), Chunk("data", samples))

    assertContentEquals(samples, pcmFromWav(wav))
  }

  @Test
  fun `a fmt chunk longer than 16 bytes is accepted`() {
    val wav = wav(fmt(extra = le16(0)), Chunk("data", samples))

    assertContentEquals(samples, pcmFromWav(wav))
  }

  @Test
  fun `input that is not RIFF is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(), Chunk("data", samples), riff = "RIFX"))
    }
  }

  @Test
  fun `a RIFF that is not WAVE is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(), Chunk("data", samples), wave = "AVI "))
    }
  }

  @Test
  fun `a non-PCM format tag is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(formatTag = 3), Chunk("data", samples)))
    }
  }

  @Test
  fun `stereo audio is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(channels = 2), Chunk("data", samples)))
    }
  }

  @Test
  fun `a sample rate other than the engine's is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(sampleRate = 48_000), Chunk("data", samples)))
    }
  }

  @Test
  fun `a sample width other than 16 bits is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(bitsPerSample = 8), Chunk("data", samples)))
    }
  }

  @Test
  fun `a fmt chunk shorter than 16 bytes is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(Chunk("fmt ", ByteArray(14)), Chunk("data", samples)))
    }
  }

  @Test
  fun `a data chunk before any fmt chunk is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(Chunk("data", samples), fmt()))
    }
  }

  @Test
  fun `a wav without a data chunk is rejected`() {
    assertFailsWith<InvalidWavException> { pcmFromWav(wav(fmt())) }
  }

  @Test
  fun `a data chunk declaring more bytes than remain is rejected`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(), Chunk("data", samples, declaredSize = samples.size + 2)))
    }
  }

  @Test
  fun `a chunk header cut short is rejected`() {
    val whole = wav(fmt(), Chunk("data", samples))
    val cut = whole.copyOf(12 + 8 + 16 + 5)

    assertFailsWith<InvalidWavException> { pcmFromWav(cut) }
  }

  @Test
  fun `input shorter than the RIFF header is rejected`() {
    assertFailsWith<InvalidWavException> { pcmFromWav(ByteArray(7)) }
  }

  @Test
  fun `a chunk size beyond the signed 32-bit range is rejected rather than wrapping`() {
    assertFailsWith<InvalidWavException> {
      pcmFromWav(wav(fmt(), Chunk("data", samples, declaredSize = -2)))
    }
  }
}
