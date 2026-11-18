package io.github.omochice.vvtts

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Typeface
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
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
  private val consent by lazy { ConsentStore(this) }
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()

  private lateinit var status: TextView
  private lateinit var downloadButton: Button
  private var dialog: AlertDialog? = null
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
    downloadButton =
        Button(this).apply {
          setText(R.string.button_download)
          setOnClickListener { startDownload() }
        }
    val termsButton =
        Button(this).apply {
          setText(R.string.button_terms)
          setOnClickListener { showTerms(forConsent = false) }
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
    dialog?.dismiss()
    executor.shutdownNow()
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
    val agreed =
        catalog.onnxRuntimeReleaseApiUrl?.let {
          consent.isGiven(consentFingerprint(catalog.terms, it))
        }
    if (agreed == true) requestNotificationsThenSchedule() else showTerms(forConsent = true)
  }

  private fun showTerms(forConsent: Boolean) {
    val loading =
        show(
            AlertDialog.Builder(this)
                .setTitle(R.string.terms_title)
                .setMessage(R.string.terms_loading)
                .setNegativeButton(R.string.terms_cancel, null)
        )
    executor.execute {
      val loaded =
          try {
            Result.success(
                TermsLoader(catalog, layout).load() to
                    installer.bytesToDownload(DOWNLOADED_VOICE_MODEL_FILE_NAME)
            )
          } catch (e: IOException) {
            Log.w(TAG, "cannot load the terms", e)
            Result.failure(e)
          }
      runOnUiThread {
        // Compared by identity, because a load the user cancelled must not fill the dialog of a
        // later one.
        if (isDestroyed || dialog !== loading || !loading.isShowing) return@runOnUiThread
        loaded.fold(
            { (terms, bytes) -> showLoadedTerms(terms, bytes.takeIf { forConsent }) },
            { showTermsError(forConsent) },
        )
      }
    }
  }

  /** Shows [terms], asking for consent to download [downloadBytes] unless it is null. */
  private fun showLoadedTerms(terms: Terms, downloadBytes: Long?) {
    val documents =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          val gap = dp(24)
          setPadding(gap, dp(8), gap, 0)
          if (downloadBytes != null) {
            addView(
                body(
                    getString(
                        R.string.terms_download_size,
                        Formatter.formatShortFileSize(this@SettingsActivity, downloadBytes),
                    )
                )
            )
          }
          for (document in terms.voiceModelTerms) {
            addView(heading(document.title))
            addView(body(document.text))
          }
          addView(heading(getString(R.string.terms_onnx_runtime_heading)))
          addView(body(terms.onnxRuntimeTerms))
        }
    val builder =
        AlertDialog.Builder(this)
            .setTitle(R.string.terms_title)
            .setView(ScrollView(this).apply { addView(documents) })
    if (downloadBytes == null) {
      builder.setPositiveButton(R.string.terms_close, null)
    } else {
      builder
          .setPositiveButton(R.string.terms_agree) { _, _ ->
            consent.record(terms.fingerprint)
            requestNotificationsThenSchedule()
          }
          .setNegativeButton(R.string.terms_cancel, null)
    }
    show(builder)
  }

  private fun showTermsError(forConsent: Boolean) {
    show(
        AlertDialog.Builder(this)
            .setTitle(R.string.terms_title)
            .setMessage(R.string.terms_error)
            .setPositiveButton(R.string.terms_retry) { _, _ -> showTerms(forConsent) }
            .setNegativeButton(R.string.terms_cancel, null)
    )
  }

  private fun show(builder: AlertDialog.Builder): AlertDialog {
    dialog?.dismiss()
    return builder.show().also { dialog = it }
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

  private fun heading(text: String) =
      TextView(this).apply {
        this.text = text
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(16), 0, dp(4))
      }

  private fun body(text: String) =
      TextView(this).apply {
        this.text = text
        setTextIsSelectable(true)
      }

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  private companion object {
    const val REQUEST_NOTIFICATIONS = 1
  }
}
