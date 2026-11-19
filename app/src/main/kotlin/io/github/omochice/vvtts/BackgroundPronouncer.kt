package io.github.omochice.vvtts

import android.app.Activity
import android.util.Log
import kotlin.concurrent.thread

/**
 * Lets the screens of [activity] use the shared engine as a [WordPronouncer] without blocking the
 * UI thread, speaking with the voice [voice] chooses.
 *
 * The UI thread must never wait for the engine, which a synthesis in progress holds for seconds.
 *
 * @property voice returns the voice to pronounce with, or null when no voice model is installed.
 */
internal class BackgroundPronouncer(
    private val activity: Activity,
    val voice: () -> SpeakableVoice?,
) {
  /**
   * Runs [work] with the engine on a thread of its own, then hands what it returned or threw to
   * [deliver] on the UI thread.
   */
  // Any exception means the engine cannot be used right now, which the caller shows the same way
  // whatever its type, and an uncaught one would end the process.
  // It is logged by type only, because VOICEVOX's messages quote the user's reading.
  @Suppress("TooGenericExceptionCaught")
  fun <R> run(work: (WordPronouncer) -> R, deliver: (Result<R>) -> Unit) {
    thread(name = "pronouncer", isDaemon = true) {
      val result =
          try {
            Result.success(SharedSpeechEngine.usePronouncer(activity, work))
          } catch (e: Exception) {
            Log.w(TAG, "cannot pronounce: ${e.javaClass.name}")
            Result.failure(e)
          }
      // Not delivered to a destroyed activity, whose dialogs can no longer be shown.
      activity.runOnUiThread { if (!activity.isDestroyed) deliver(result) }
    }
  }
}
