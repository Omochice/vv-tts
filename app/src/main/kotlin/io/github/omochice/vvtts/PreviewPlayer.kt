package io.github.omochice.vvtts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Plays short speech in the format [SpeechEngine.synthesize] returns, one sound at a time.
 *
 * Used from the UI thread only.
 */
internal class PreviewPlayer {
  private var track: AudioTrack? = null

  /**
   * Stops what is playing and plays [pcm] instead.
   *
   * @throws UnsupportedOperationException if the system cannot make a track for it.
   */
  fun play(pcm: ByteArray) {
    stop()
    if (pcm.isEmpty()) return
    // A static track, because the whole word is at hand and lasts a few seconds at most, so
    // streaming it from a thread of its own would add a thread without starting any sooner.
    val played =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SpeechEngine.SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size)
            .build()
    track = played
    played.write(pcm, 0, pcm.size)
    played.play()
  }

  /** Stops what is playing, if anything, and frees its track. */
  fun stop() {
    val played = track ?: return
    track = null
    if (played.state == AudioTrack.STATE_INITIALIZED) played.stop()
    played.release()
  }
}
