package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult

/** Why an install failed, in the terms of what the user can do about it. */
enum class FailureReason {
  /** The transfer broke off, through the network or a full disk. */
  TRANSFER,

  /** The server did not hand out the file. */
  SERVER,

  /** The downloaded file was not the pinned one. */
  CORRUPT,

  /** A downloaded file could not be put in place on this device. */
  STORAGE,

  /** This device's CPU has no ONNX Runtime build. */
  UNSUPPORTED_DEVICE,
}

/**
 * Returns why [failure] happened.
 *
 * @throws IllegalArgumentException if [failure] carries a download result that is not a failure.
 */
fun failureReasonOf(failure: InstallResult.Failure): FailureReason =
    when (failure) {
      is InstallResult.NoOnnxRuntimeFor -> FailureReason.UNSUPPORTED_DEVICE
      is InstallResult.PlacementFailed -> FailureReason.STORAGE
      is InstallResult.DownloadFailed ->
          when (val result = failure.result) {
            is DownloadResult.TransferFailed -> FailureReason.TRANSFER
            is DownloadResult.UnexpectedStatus -> FailureReason.SERVER
            DownloadResult.Corrupt -> FailureReason.CORRUPT
            DownloadResult.Installed,
            DownloadResult.Stopped ->
                throw IllegalArgumentException("$result is not a download failure")
          }
    }
