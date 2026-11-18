package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.InstallResult

/** What the settings screen tells the user about the speech assets. */
sealed interface SettingsStatus {
  /** Everything needed to speak is present. */
  data object Installed : SettingsStatus

  /** Something is missing and no download is under way. */
  data object NotInstalled : SettingsStatus

  /** A download is scheduled but has not started, such as while there is no network. */
  data object Pending : SettingsStatus

  /** A download has started but has not reported how much it has to fetch. */
  data object Preparing : SettingsStatus

  /** A download is under way; both counts are in bytes. */
  data class Downloading(val doneBytes: Long, val totalBytes: Long) : SettingsStatus

  /** A transfer broke off, and the download waits before attempt [attempt] of [maxAttempts]. */
  data class Retrying(val attempt: Int, val maxAttempts: Int) : SettingsStatus

  /** The latest download was stopped before it finished. */
  data object Stopped : SettingsStatus

  /** The latest download failed for [reason]. */
  data class Failed(val reason: FailureReason) : SettingsStatus

  /** Whether the user may start a download from this status. */
  val allowsDownload: Boolean
    get() = this is NotInstalled || this is Stopped || this is Failed
}

/**
 * Decides what the settings screen shows.
 *
 * @param state the state of the download in this process.
 * @param isJobPending whether the system holds a scheduled download job, which outlives the
 *   process.
 * @param isInstalled whether the engine can speak with at least one voice.
 */
fun settingsStatusOf(
    state: InstallState,
    isJobPending: Boolean,
    isInstalled: Boolean,
): SettingsStatus =
    when {
      state is InstallState.Running && state.totalBytes > 0 ->
          SettingsStatus.Downloading(state.doneBytes, state.totalBytes)
      state is InstallState.Running -> SettingsStatus.Preparing
      state is InstallState.Retrying -> SettingsStatus.Retrying(state.attempt, state.maxAttempts)
      isJobPending -> SettingsStatus.Pending
      isInstalled -> SettingsStatus.Installed
      state is InstallState.Finished -> finishedStatusOf(state.result)
      else -> SettingsStatus.NotInstalled
    }

private fun finishedStatusOf(result: InstallResult): SettingsStatus =
    when (result) {
      is InstallResult.Failure -> SettingsStatus.Failed(failureReasonOf(result))
      InstallResult.Stopped -> SettingsStatus.Stopped
      InstallResult.Installed -> SettingsStatus.NotInstalled
    }
