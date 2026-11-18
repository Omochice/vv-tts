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
import io.github.omochice.vvtts.dictionary.UserDictionary
import io.github.omochice.vvtts.dictionary.UserDictionaryFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lets the user download voice models after agreeing to their terms, choose the default voice, read
 * the terms again later, and keep a dictionary of how words are read.
 */
class SettingsActivity : Activity() {
  private val catalog by lazy { AssetCatalog.load() }
  private val layout by lazy { AssetLayout(filesDir) }
  private val installer by lazy {
    AssetInstaller(catalog, layout, Build.SUPPORTED_ABIS.first())
  }

  private lateinit var status: TextView
  private lateinit var defaultVoice: DefaultVoicePicker
  private lateinit var voiceModelRows: VoiceModelRows
  private lateinit var terms: TermsDialog
  private lateinit var dictionary: DictionarySection
  private var pendingVoiceModel: String? = null

  /** The voice model to schedule once the user has answered the notification permission request. */
  private var awaitingPermission: String? = null

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
    awaitingPermission = savedInstanceState?.getString(STATE_AWAITING_PERMISSION)
    status = TextView(this).apply { textSize = 16f }
    terms = TermsDialog(this, catalog, installer, ::requestNotificationsThenSchedule)
    dictionary = DictionarySection(this, UserDictionary(UserDictionaryFile(filesDir)))
    defaultVoice = DefaultVoicePicker(this)
    voiceModelRows =
        VoiceModelRows(this, catalog.voiceModels) { terms.requestConsent(it.file.fileName) }
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
          addView(termsButton)
          addView(defaultVoice.view)
          addView(headingView(getString(R.string.voice_models_heading)))
          addView(TextView(this@SettingsActivity).apply { setText(R.string.voice_models_note) })
          addView(voiceModelRows.view)
          addView(dictionary.view)
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
    pendingVoiceModel = AssetDownloadJobService.pendingVoiceModel(this)
    render()
    dictionary.refresh()
  }

  override fun onStop() {
    AssetDownloadJobService.state.removeListener(stateListener)
    super.onStop()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_AWAITING_PERMISSION, awaitingPermission)
  }

  override fun onDestroy() {
    terms.close()
    dictionary.close()
    super.onDestroy()
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != REQUEST_NOTIFICATIONS) return
    // Scheduled whatever the answer, because the download works without notifications; the
    // system still lists it among running tasks.
    awaitingPermission?.let(::schedule)
    awaitingPermission = null
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
      pendingVoiceModel =
          if (current.result == InstallResult.Stopped) {
            AssetDownloadJobService.pendingVoiceModel(this)
          } else {
            null
          }
    }
  }

  private fun render() {
    val installed = installedVoices(catalog.voiceModels, layout)
    val current =
        settingsStatusOf(
            AssetDownloadJobService.state.current,
            pendingVoiceModel,
            installed.isNotEmpty(),
        )
    status.text = describe(current)
    defaultVoice.show(installed)
    voiceModelRows.update(layout::isInstalled, current.allowsDownload)
  }

  private fun describe(current: SettingsStatus): String =
      when (current) {
        SettingsStatus.Installed -> getString(R.string.status_installed)
        SettingsStatus.NotInstalled -> getString(R.string.status_not_installed)
        is SettingsStatus.Pending ->
            getString(R.string.status_pending, voiceModelName(current.voiceModelFileName))
        is SettingsStatus.Preparing ->
            getString(R.string.status_preparing, voiceModelName(current.voiceModelFileName))
        is SettingsStatus.Downloading ->
            getString(
                R.string.status_running,
                voiceModelName(current.voiceModelFileName),
                wholePercent(current.doneBytes, current.totalBytes),
                Formatter.formatShortFileSize(this, current.doneBytes),
                Formatter.formatShortFileSize(this, current.totalBytes),
            )
        is SettingsStatus.Retrying ->
            getString(
                R.string.status_retrying,
                voiceModelName(current.voiceModelFileName),
                current.attempt,
                current.maxAttempts,
            )
        SettingsStatus.Stopped -> getString(R.string.status_stopped)
        is SettingsStatus.Failed -> getString(current.reason.messageRes())
      }

  /** Names a voice model by the characters it holds, which the user knows it by. */
  private fun voiceModelName(fileName: String): String {
    val voices = catalog.voiceModels.find { it.file.fileName == fileName }?.voices.orEmpty()
    val characters = characterStylesOf(voices).joinToString("、") { it.character }
    return characters.ifEmpty { fileName }
  }

  private fun requestNotificationsThenSchedule(voiceModelFileName: String) {
    if (
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    ) {
      schedule(voiceModelFileName)
    } else {
      awaitingPermission = voiceModelFileName
      requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }
  }

  private fun schedule(voiceModelFileName: String) {
    val bytes = installer.bytesToDownload(voiceModelFileName)
    if (!AssetDownloadJobService.schedule(this, voiceModelFileName, bytes)) {
      Log.w(TAG, "the download job was not scheduled")
      Toast.makeText(this, R.string.schedule_failed, Toast.LENGTH_LONG).show()
    }
    pendingVoiceModel = AssetDownloadJobService.pendingVoiceModel(this)
    render()
  }

  private companion object {
    const val REQUEST_NOTIFICATIONS = 1
    const val STATE_AWAITING_PERMISSION = "awaitingPermission"
  }
}
