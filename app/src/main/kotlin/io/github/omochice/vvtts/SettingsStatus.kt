package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.InstallResult

/** What the settings screen tells the user about the speech assets. */
sealed interface SettingsStatus {
  /**
   * At least one voice can speak, and the latest download neither is under way nor failed or
   * stopped.
   */
  data object Installed : SettingsStatus

  /** No voice can speak, and the latest download neither is under way nor failed or stopped. */
  data object NotInstalled : SettingsStatus

  /**
   * A download of [voiceModelFileName] is scheduled but has not started, such as while there is no
   * network.
   */
  data class Pending(val voiceModelFileName: String) : SettingsStatus

  /**
   * A download of [voiceModelFileName] has started but has not reported how much it has to fetch.
   */
  data class Preparing(val voiceModelFileName: String) : SettingsStatus

  /**
   * A download of [voiceModelFileName] is under way, with [doneBytes] of [totalBytes] bytes done.
   */
  data class Downloading(
      val voiceModelFileName: String,
      val doneBytes: Long,
      val totalBytes: Long,
  ) : SettingsStatus

  /**
   * A transfer broke off, and the download of [voiceModelFileName] waits before attempt [attempt]
   * of [maxAttempts].
   */
  data class Retrying(val voiceModelFileName: String, val attempt: Int, val maxAttempts: Int) :
      SettingsStatus

  /** The latest download was stopped before it finished. */
  data object Stopped : SettingsStatus

  /** The latest download failed for [reason]. */
  data class Failed(val reason: FailureReason) : SettingsStatus

  /** Whether the user may start a download from this status. */
  val allowsDownload: Boolean
    get() = this is NotInstalled || this is Installed || this is Stopped || this is Failed
}

/**
 * Decides what the settings screen shows.
 *
 * @param state the state of the download in this process.
 * @param pendingVoiceModel the voice model of the download job the system holds, which outlives the
 *   process; null when it holds none.
 * @param isInstalled whether the engine can speak with at least one voice.
 */
fun settingsStatusOf(
    state: InstallState,
    pendingVoiceModel: String?,
    isInstalled: Boolean,
): SettingsStatus =
    when {
      state is InstallState.Running && state.totalBytes > 0 ->
          SettingsStatus.Downloading(state.voiceModelFileName, state.doneBytes, state.totalBytes)
      state is InstallState.Running -> SettingsStatus.Preparing(state.voiceModelFileName)
      state is InstallState.Retrying ->
          SettingsStatus.Retrying(state.voiceModelFileName, state.attempt, state.maxAttempts)
      pendingVoiceModel != null -> SettingsStatus.Pending(pendingVoiceModel)
      // Ahead of isInstalled, because with other voice models installed the user would otherwise
      // never learn that the one just asked for did not arrive.
      state is InstallState.Finished && state.result is InstallResult.Failure ->
          SettingsStatus.Failed(failureReasonOf(state.result))
      state is InstallState.Finished && state.result == InstallResult.Stopped ->
          SettingsStatus.Stopped
      isInstalled -> SettingsStatus.Installed
      else -> SettingsStatus.NotInstalled
    }
