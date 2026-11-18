package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.DownloadResult
import io.github.omochice.vvtts.assets.InstallResult

/**
 * Tells whether an install attempt that ended with [result] is worth repeating.
 *
 * Only a broken-off transfer qualifies, because every other failure would repeat unchanged until
 * the app or the device changes.
 */
fun isRetryable(result: InstallResult): Boolean =
    result is InstallResult.DownloadFailed && result.result is DownloadResult.TransferFailed

/**
 * Runs [attempt] until it ends with a result that [isRetryable] rejects, at most [maxAttempts]
 * times, and returns the last result.
 *
 * @param pause called between two attempts; it may return early once a stop is requested.
 * @param isStopRequested consulted after every pause; once true, the result is
 *   [InstallResult.Stopped] without another attempt.
 * @param onRetry receives the number of the attempt that just failed, counting from 1, and its
 *   failure, before the pause that follows it.
 */
fun installWithRetries(
    maxAttempts: Int,
    attempt: () -> InstallResult,
    pause: () -> Unit,
    isStopRequested: () -> Boolean,
    onRetry: (failedAttempt: Int, failure: InstallResult.Failure) -> Unit,
): InstallResult {
  var number = 1
  while (true) {
    val result = attempt()
    if (number >= maxAttempts || result !is InstallResult.Failure || !isRetryable(result)) {
      return result
    }
    onRetry(number, result)
    pause()
    if (isStopRequested()) return InstallResult.Stopped
    number++
  }
}
