package io.github.omochice.vvtts

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetInstaller
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.assets.InstallResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Downloads the speech assets as a user-initiated data transfer job, so the download goes on after
 * the app is closed.
 */
class AssetDownloadJobService : JobService() {
  private val notifications by lazy { InstallNotifications(this) }

  // Replaced on every start rather than reset, so that a stopped run still finishing on its thread
  // never sees the signal of the run that replaced it.
  // A latch rather than a flag, so that a pause between attempts ends as soon as a stop arrives.
  @Volatile private var stopSignal = CountDownLatch(1)

  override fun onStartJob(params: JobParameters): Boolean {
    Log.i(TAG, "download job started")
    notifications.createChannel()
    setNotification(
        params,
        NOTIFICATION_ID,
        notifications.running(null),
        JOB_END_NOTIFICATION_POLICY_DETACH,
    )
    val stop = CountDownLatch(1).also { stopSignal = it }
    thread(name = "asset-download") { download(params, stop) }
    return true
  }

  override fun onStopJob(params: JobParameters): Boolean {
    Log.i(TAG, "download job stopped, reason ${params.stopReason}")
    stopSignal.countDown()
    return true
  }

  private fun download(params: JobParameters, stop: CountDownLatch) {
    val run = state.startRun(DOWNLOADED_VOICE_MODEL_FILE_NAME)
    val isStopRequested = { stop.count == 0L }
    val installer =
        AssetInstaller(AssetCatalog.load(), AssetLayout(filesDir), Build.SUPPORTED_ABIS.first())
    // Retried within the run rather than through jobFinished(params, true), because a job
    // rescheduled that way was seen never to run again although all its constraints were met.
    val result =
        installWithRetries(
            MAX_ATTEMPTS,
            attempt = { attempt(params, installer, run, isStopRequested) },
            pause = { stop.await(RETRY_PAUSE_SECONDS, TimeUnit.SECONDS) },
            isStopRequested = isStopRequested,
            onRetry = { failedAttempt, failure ->
              Log.w(TAG, "download attempt $failedAttempt of $MAX_ATTEMPTS failed: $failure")
              run.retrying(failedAttempt + 1, MAX_ATTEMPTS)
            },
        )
    Log.i(TAG, "download finished: $result")
    run.finish(result)
    // The system has taken this run back and restarts it once its constraints hold again, so
    // neither the notification nor jobFinished belongs to it any more.
    if (isStopRequested()) return
    val final =
        when (result) {
          InstallResult.Installed -> notifications.installed()
          is InstallResult.Failure -> notifications.failed(failureReasonOf(result))
          InstallResult.Stopped -> return
        }
    // Handed to the job rather than posted after jobFinished, so that the replacement cannot race
    // the system detaching the progress notification.
    setNotification(params, NOTIFICATION_ID, final, JOB_END_NOTIFICATION_POLICY_DETACH)
    jobFinished(params, false)
  }

  private fun attempt(
      params: JobParameters,
      installer: AssetInstaller,
      run: InstallStateHolder.Run,
      isStopRequested: () -> Boolean,
  ): InstallResult {
    var firstProgressLogged = false
    var shownPercent = -1
    return installer.install(
        DOWNLOADED_VOICE_MODEL_FILE_NAME,
        { doneBytes, totalBytes ->
          run.progress(doneBytes, totalBytes)
          if (!firstProgressLogged) {
            firstProgressLogged = true
            Log.i(TAG, "download progress first done=$doneBytes total=$totalBytes")
          }
          val percent = wholePercent(doneBytes, totalBytes)
          if (percent != shownPercent && !isStopRequested()) {
            shownPercent = percent
            showProgress(params, percent, doneBytes)
          }
        },
        isStopRequested,
    )
  }

  private fun showProgress(params: JobParameters, percent: Int, doneBytes: Long) {
    setNotification(
        params,
        NOTIFICATION_ID,
        notifications.running(percent),
        JOB_END_NOTIFICATION_POLICY_DETACH,
    )
    updateTransferredNetworkBytes(params, doneBytes, 0)
  }

  /** Scheduling and observing the download job. */
  companion object {
    /** The state of the download in this process. */
    val state = InstallStateHolder()

    private const val JOB_ID = 1
    private const val NOTIFICATION_ID = 1
    private const val MAX_ATTEMPTS = 5
    private const val RETRY_PAUSE_SECONDS = 10L

    /**
     * Schedules the download, which must be done while the app is visible to the user.
     *
     * @param estimatedBytes the bytes still to download.
     * @return whether the system accepted the job.
     */
    fun schedule(context: Context, estimatedBytes: Long): Boolean {
      val job =
          JobInfo.Builder(JOB_ID, ComponentName(context, AssetDownloadJobService::class.java))
              .setUserInitiated(true)
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setEstimatedNetworkBytes(estimatedBytes, 0)
              .build()
      return try {
        scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS
      } catch (e: SecurityException) {
        Log.w(TAG, "the download job was refused", e)
        false
      }
    }

    /** Tells whether the system holds the download job, scheduled or running. */
    fun isPending(context: Context): Boolean = scheduler(context).getPendingJob(JOB_ID) != null

    private fun scheduler(context: Context): JobScheduler =
        context.getSystemService(JobScheduler::class.java)
  }
}
