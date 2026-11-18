package io.github.omochice.vvtts

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val RIFF_HEADER_BYTES = 12
private const val CHUNK_HEADER_BYTES = 8
private const val FMT_MIN_BYTES = 16
private const val FORMAT_PCM = 1
private const val BITS_PER_SAMPLE = 16
private const val FOURCC_BYTES = 4
private const val WAVE_ID_OFFSET = 8
private const val CHUNK_SIZE_OFFSET = 4
private const val FMT_CHANNELS_OFFSET = 2
private const val FMT_SAMPLE_RATE_OFFSET = 4
private const val FMT_BITS_PER_SAMPLE_OFFSET = 14

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
  if (fourCc(wav, WAVE_ID_OFFSET) != "WAVE") throw InvalidWavException("not a WAVE file")
  var formatChecked = false
  var offset = RIFF_HEADER_BYTES
  while (offset < wav.size) {
    if (wav.size - offset < CHUNK_HEADER_BYTES) throw InvalidWavException("truncated chunk header")
    val id = fourCc(wav, offset)
    val size = buffer.getInt(offset + CHUNK_SIZE_OFFSET).toUInt().toLong()
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
  val format =
      WavFormat(
          formatTag = buffer.getShort(start).toUShort().toInt(),
          channels = buffer.getShort(start + FMT_CHANNELS_OFFSET).toUShort().toInt(),
          sampleRate = buffer.getInt(start + FMT_SAMPLE_RATE_OFFSET),
          bitsPerSample = buffer.getShort(start + FMT_BITS_PER_SAMPLE_OFFSET).toUShort().toInt(),
      )
  if (format != EXPECTED_FORMAT) {
    throw InvalidWavException(
        "format ${format.formatTag}, ${format.channels} channels, ${format.sampleRate} Hz, " +
            "${format.bitsPerSample} bits"
    )
  }
}

private data class WavFormat(
    val formatTag: Int,
    val channels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
)

private val EXPECTED_FORMAT =
    WavFormat(
        formatTag = FORMAT_PCM,
        channels = 1,
        sampleRate = SpeechEngine.SAMPLE_RATE_HZ,
        bitsPerSample = BITS_PER_SAMPLE,
    )

private fun fourCc(bytes: ByteArray, offset: Int) =
    String(bytes, offset, FOURCC_BYTES, Charsets.US_ASCII)
