package io.github.omochice.vvtts

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetInstaller
import io.github.omochice.vvtts.assets.AssetLayout
import io.github.omochice.vvtts.assets.InstallResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lets the user download the speech assets after agreeing to their terms, and read the terms again
 * later.
 */
class SettingsActivity : Activity() {
  private val catalog by lazy { AssetCatalog.load() }
  private val layout by lazy { AssetLayout(filesDir) }
  private val installer by lazy {
    AssetInstaller(catalog, layout, Build.SUPPORTED_ABIS.first())
  }

  private lateinit var status: TextView
  private lateinit var downloadButton: Button
  private lateinit var terms: TermsDialog
  private var jobPending = false

  private val renderPosted = AtomicBoolean(false)
  private val stateListener: (InstallState) -> Unit = {
    // Coalesced, because a download reports progress for every buffer it writes.
    if (renderPosted.compareAndSet(false, true)) {
      runOnUiThread {
        renderPosted.set(false)
        if (!isDestroyed) {
          forgetFinishedJob()
          render()
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    status = TextView(this).apply { textSize = 16f }
    terms = TermsDialog(this, catalog, layout, installer) { requestNotificationsThenSchedule() }
    downloadButton =
        Button(this).apply {
          setText(R.string.button_download)
          setOnClickListener { startDownload() }
        }
    val termsButton =
        Button(this).apply {
          setText(R.string.button_terms)
          setOnClickListener { terms.show(forVoiceModel = null) }
        }
    val content =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          val gap = dp(16)
          setPadding(gap, gap, gap, gap)
          addView(status)
          addView(downloadButton)
          addView(termsButton)
        }
    setContentView(
        ScrollView(this).apply {
          clipToPadding = false
          addView(content)
          setOnApplyWindowInsetsListener { view, insets ->
            val bars =
                insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsets.CONSUMED
          }
        }
    )
  }

  override fun onStart() {
    super.onStart()
    AssetDownloadJobService.state.addListener(stateListener)
    jobPending = AssetDownloadJobService.isPending(this)
    render()
  }

  override fun onStop() {
    AssetDownloadJobService.state.removeListener(stateListener)
    super.onStop()
  }

  override fun onDestroy() {
    terms.close()
    super.onDestroy()
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    // Scheduled whatever the answer, because the download works without notifications; the
    // system still lists it among running tasks.
    if (requestCode == REQUEST_NOTIFICATIONS) schedule()
  }

  /**
   * Stops counting the job of a run that just ended as pending, which the system keeps listing
   * until it has processed the end.
   *
   * A run that ends by itself never asks to be rescheduled, so only a stopped run, whose
   * rescheduling the system decides, is looked up again.
   */
  private fun forgetFinishedJob() {
    val current = AssetDownloadJobService.state.current
    if (current is InstallState.Finished) {
      jobPending =
          current.result == InstallResult.Stopped && AssetDownloadJobService.isPending(this)
    }
  }

  private fun render() {
    val current =
        settingsStatusOf(
            AssetDownloadJobService.state.current,
            jobPending,
            installedVoices(catalog.voiceModels, layout).isNotEmpty(),
        )
    status.text = describe(current)
    downloadButton.isEnabled = current.allowsDownload
  }

  private fun describe(current: SettingsStatus): String =
      when (current) {
        SettingsStatus.Installed -> getString(R.string.status_installed)
        SettingsStatus.NotInstalled -> getString(R.string.status_not_installed)
        SettingsStatus.Pending -> getString(R.string.status_pending)
        SettingsStatus.Preparing -> getString(R.string.status_preparing)
        is SettingsStatus.Downloading ->
            getString(
                R.string.status_running,
                wholePercent(current.doneBytes, current.totalBytes),
                Formatter.formatShortFileSize(this, current.doneBytes),
                Formatter.formatShortFileSize(this, current.totalBytes),
            )
        is SettingsStatus.Retrying ->
            getString(R.string.status_retrying, current.attempt, current.maxAttempts)
        SettingsStatus.Stopped -> getString(R.string.status_stopped)
        is SettingsStatus.Failed -> getString(current.reason.messageRes())
      }

  private fun startDownload() {
    terms.requestConsent(DOWNLOADED_VOICE_MODEL_FILE_NAME)
  }

  private fun requestNotificationsThenSchedule() {
    if (
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    ) {
      schedule()
    } else {
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }
  }

  private fun schedule() {
    val bytes = installer.bytesToDownload(DOWNLOADED_VOICE_MODEL_FILE_NAME)
    if (!AssetDownloadJobService.schedule(this, bytes)) {
      Log.w(TAG, "the download job was not scheduled")
      Toast.makeText(this, R.string.schedule_failed, Toast.LENGTH_LONG).show()
    }
    jobPending = AssetDownloadJobService.isPending(this)
    render()
  }

  private companion object {
    const val REQUEST_NOTIFICATIONS = 1
  }
}
