package io.github.omochice.vvtts.assets

import java.io.File
import java.io.IOException

/**
 * What became of one [AssetDownloader.download] call.
 *
 * Every outcome other than [Installed] leaves the decision to try again with the caller; a single
 * call never retries.
 */
sealed interface DownloadResult {
  /** The verified file is at the target path, read-only. */
  data object Installed : DownloadResult

  /** The call ended without the file and without a stop being requested. */
  sealed interface Failure : DownloadResult

  /**
   * The downloaded bytes did not match the pin and were deleted, so the next attempt starts over.
   *
   * Repeating this outcome means the pin or the server is wrong, not the network.
   */
  data object Corrupt : Failure

  /** A stop was requested; the bytes received so far are kept and the next attempt resumes. */
  data object Stopped : DownloadResult

  /**
   * The server answered with [statusCode] instead of the file; any bytes received earlier are kept.
   */
  data class UnexpectedStatus(val statusCode: Int) : Failure

  /**
   * The transfer broke off; the bytes received so far are kept and the next attempt resumes.
   *
   * [cause] can also be a local write failure, such as a full disk.
   */
  data class TransferFailed(val cause: IOException) : Failure
}

/** Downloads pinned remote files, resuming interrupted transfers. */
class AssetDownloader(private val http: HttpRangeFetcher = HttpRangeFetcher()) {
  /**
   * Makes [target] hold the content pinned by [remote].
   *
   * Bytes are collected in a file named after [target] with a `.part` suffix in the same directory,
   * which survives between calls so that a later call resumes it.
   *
   * An existing [target] is taken as already installed.
   *
   * @param onProgress receives the bytes held so far and the pinned total, both in bytes.
   * @param isStopRequested consulted before every network read.
   * @throws IOException if the partial file cannot be read for verification or cannot be moved to
   *   [target].
   */
  fun download(
      remote: RemoteFile,
      target: File,
      onProgress: (bytesSoFar: Long, totalBytes: Long) -> Unit = { _, _ -> },
      isStopRequested: () -> Boolean,
  ): DownloadResult {
    // Not re-verified: only verified files are ever moved to the target,
    // and re-hashing every installed model on each call would cost seconds per file.
    if (target.exists()) return DownloadResult.Installed
    val partial = File(target.parentFile, target.name + ".part")
    if (partial.length() < remote.sizeBytes) {
      val end =
          try {
            http.fetch(remote.url, partial, remote.sizeBytes, onProgress, isStopRequested)
          } catch (e: UnexpectedStatusException) {
            return DownloadResult.UnexpectedStatus(e.statusCode)
          } catch (e: IOException) {
            return DownloadResult.TransferFailed(e)
          }
      if (end == FetchEnd.STOPPED) return DownloadResult.Stopped
    }
    if (!remote.isContentOf(partial)) {
      partial.delete()
      return DownloadResult.Corrupt
    }
    installReadOnly(partial, target)
    return DownloadResult.Installed
  }
}
