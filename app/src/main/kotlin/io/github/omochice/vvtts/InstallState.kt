package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.InstallResult
import java.util.concurrent.CopyOnWriteArrayList

/** Where the asset download of this process stands. */
sealed interface InstallState {
  /** No download has run in this process. */
  data object Idle : InstallState

  /**
   * A download is under way.
   *
   * @property voiceModelFileName the voice model the run installs, such as `0.vvm`.
   * @property doneBytes the bytes downloaded so far by this run.
   * @property totalBytes the bytes this run has to download; 0 until it is known.
   */
  data class Running(
      val voiceModelFileName: String,
      val doneBytes: Long,
      val totalBytes: Long,
  ) : InstallState

  /**
   * A transfer broke off, and the run for [voiceModelFileName] waits before attempt number
   * [attempt] of [maxAttempts], counting from 1.
   */
  data class Retrying(val voiceModelFileName: String, val attempt: Int, val maxAttempts: Int) :
      InstallState

  /** The latest run, which was for [voiceModelFileName], ended with [result]. */
  data class Finished(val voiceModelFileName: String, val result: InstallResult) : InstallState
}

/**
 * Holds the [InstallState] that the download job writes and the settings screen observes.
 *
 * Listeners are called on the thread that changed the state.
 */
class InstallStateHolder {
  private val listeners = CopyOnWriteArrayList<(InstallState) -> Unit>()
  private var latest: Run? = null

  /** The state most recently reported by the latest run. */
  @Volatile
  var current: InstallState = InstallState.Idle
    private set

  /** Registers [listener] for every later change. */
  fun addListener(listener: (InstallState) -> Unit) {
    listeners += listener
  }

  /** Stops calling [listener]. */
  fun removeListener(listener: (InstallState) -> Unit) {
    listeners -= listener
  }

  /**
   * Begins a run that installs the voice model named [voiceModelFileName], which supersedes any
   * earlier run, and reports it as running.
   */
  fun startRun(voiceModelFileName: String): Run =
      synchronized(this) {
        Run(voiceModelFileName).also {
          latest = it
          publish(InstallState.Running(voiceModelFileName, 0, 0))
        }
      }

  private fun report(run: Run, state: InstallState) {
    synchronized(this) { if (run === latest) publish(state) }
  }

  // Published under the lock so that listeners see the changes in the order they were made.
  private fun publish(state: InstallState) {
    current = state
    listeners.forEach { it(state) }
  }

  /** One download run; once a later run has started, whatever it reports is ignored. */
  inner class Run internal constructor(private val voiceModelFileName: String) {
    /** Reports the bytes downloaded so far and the total, both in bytes. */
    fun progress(doneBytes: Long, totalBytes: Long) =
        report(this, InstallState.Running(voiceModelFileName, doneBytes, totalBytes))

    /** Reports that the run waits before attempt number [attempt] of [maxAttempts]. */
    fun retrying(attempt: Int, maxAttempts: Int) =
        report(this, InstallState.Retrying(voiceModelFileName, attempt, maxAttempts))

    /** Reports how the run ended. */
    fun finish(result: InstallResult) =
        report(this, InstallState.Finished(voiceModelFileName, result))
  }
}
