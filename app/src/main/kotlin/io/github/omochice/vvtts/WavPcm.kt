package io.github.omochice.vvtts

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val RIFF_HEADER_BYTES = 12
private const val CHUNK_HEADER_BYTES = 8
private const val FMT_MIN_BYTES = 16
private const val FORMAT_PCM = 1
private const val BITS_PER_SAMPLE = 16

/** Thrown when bytes are not a WAV file holding audio in the format [SpeechEngine] promises. */
class InvalidWavException(message: String) : Exception(message)

/**
 * Returns the PCM samples of [wav], which must be 16-bit mono PCM at [SpeechEngine.SAMPLE_RATE_HZ].
 *
 * @throws InvalidWavException if [wav] is not a well-formed WAV file, is truncated, or holds audio
 *   in any other format.
 */
fun pcmFromWav(wav: ByteArray): ByteArray {
  val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
  if (wav.size < RIFF_HEADER_BYTES) throw InvalidWavException("shorter than a RIFF header")
  if (fourCc(wav, 0) != "RIFF") throw InvalidWavException("not a RIFF file")
  if (fourCc(wav, 8) != "WAVE") throw InvalidWavException("not a WAVE file")
  var formatChecked = false
  var offset = RIFF_HEADER_BYTES
  while (offset < wav.size) {
    if (wav.size - offset < CHUNK_HEADER_BYTES) throw InvalidWavException("truncated chunk header")
    val id = fourCc(wav, offset)
    val size = buffer.getInt(offset + 4).toLong() and 0xFFFF_FFFFL
    val bodyStart = offset + CHUNK_HEADER_BYTES
    if (size > wav.size - bodyStart) throw InvalidWavException("truncated '$id' chunk")
    when (id) {
      "fmt " -> {
        checkFormat(buffer, bodyStart, size.toInt())
        formatChecked = true
      }
      "data" -> {
        if (!formatChecked) throw InvalidWavException("data chunk precedes fmt chunk")
        return wav.copyOfRange(bodyStart, bodyStart + size.toInt())
      }
    }
    offset = bodyStart + size.toInt() + (size.toInt() and 1)
  }
  throw InvalidWavException("no data chunk")
}

private fun checkFormat(buffer: ByteBuffer, start: Int, size: Int) {
  if (size < FMT_MIN_BYTES) throw InvalidWavException("fmt chunk of $size bytes")
  val formatTag = buffer.getShort(start).toInt() and 0xFFFF
  val channels = buffer.getShort(start + 2).toInt() and 0xFFFF
  val sampleRate = buffer.getInt(start + 4)
  val bitsPerSample = buffer.getShort(start + 14).toInt() and 0xFFFF
  if (
      formatTag != FORMAT_PCM ||
          channels != 1 ||
          sampleRate != SpeechEngine.SAMPLE_RATE_HZ ||
          bitsPerSample != BITS_PER_SAMPLE
  ) {
    throw InvalidWavException(
        "format $formatTag, $channels channels, $sampleRate Hz, $bitsPerSample bits"
    )
  }
}

private fun fourCc(bytes: ByteArray, offset: Int) = String(bytes, offset, 4, Charsets.US_ASCII)
