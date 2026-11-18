package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.text.format.Formatter
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetInstaller
import io.github.omochice.vvtts.assets.AssetLayout
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shows the terms of the assets in [catalog] over [activity], and asks the user to agree to them
 * before a download.
 *
 * @param onAgreed called with the voice model the user may now download.
 */
internal class TermsDialog(
    private val activity: Activity,
    private val catalog: AssetCatalog,
    private val layout: AssetLayout,
    private val installer: AssetInstaller,
    private val onAgreed: (voiceModelFileName: String) -> Unit,
) {
  private val consent = ConsentStore(activity)
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private var dialog: AlertDialog? = null

  /**
   * Lets the download of the voice model named [voiceModelFileName] go ahead, at once when the user
   * already agreed to the current terms, otherwise once they agree.
   */
  fun requestConsent(voiceModelFileName: String) {
    val agreed =
        catalog.onnxRuntimeReleaseApiUrl?.let {
          consent.isGiven(consentFingerprint(catalog.terms, it))
        }
    if (agreed == true) onAgreed(voiceModelFileName) else show(forVoiceModel = voiceModelFileName)
  }

  /**
   * Shows the terms, asking for consent to download the voice model named [forVoiceModel] unless it
   * is null.
   */
  fun show(forVoiceModel: String?) {
    val loading =
        replaceDialog(
            AlertDialog.Builder(activity)
                .setTitle(R.string.terms_title)
                .setMessage(R.string.terms_loading)
                .setNegativeButton(R.string.terms_cancel, null)
        )
    executor.execute {
      val loaded =
          try {
            Result.success(
                TermsLoader(catalog, layout).load() to
                    forVoiceModel?.let { PlannedDownload(it, installer.bytesToDownload(it)) }
            )
          } catch (e: IOException) {
            Log.w(TAG, "cannot load the terms", e)
            Result.failure(e)
          }
      activity.runOnUiThread {
        // Compared by identity, because a load the user cancelled must not fill the dialog of a
        // later one.
        if (activity.isDestroyed || dialog !== loading || !loading.isShowing) {
          return@runOnUiThread
        }
        loaded.fold(
            { (terms, download) -> showLoaded(terms, download) },
            { showError(forVoiceModel) },
        )
      }
    }
  }

  /** Dismisses whatever dialog is showing and stops loading terms. */
  fun close() {
    dialog?.dismiss()
    executor.shutdownNow()
  }

  /** Shows [terms], asking for consent to [download] unless it is null. */
  private fun showLoaded(terms: Terms, download: PlannedDownload?) {
    val documents =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          val gap = activity.dp(24)
          setPadding(gap, activity.dp(8), gap, 0)
          if (download != null) {
            addView(
                body(
                    activity.getString(
                        R.string.terms_download_size,
                        Formatter.formatShortFileSize(activity, download.bytes),
                    )
                )
            )
          }
          for (document in terms.voiceModelTerms) {
            addView(activity.headingView(document.title))
            addView(body(document.text))
          }
          addView(activity.headingView(activity.getString(R.string.terms_onnx_runtime_heading)))
          addView(body(terms.onnxRuntimeTerms))
        }
    val builder =
        AlertDialog.Builder(activity)
            .setTitle(R.string.terms_title)
            .setView(ScrollView(activity).apply { addView(documents) })
    if (download == null) {
      builder.setPositiveButton(R.string.terms_close, null)
    } else {
      builder
          .setPositiveButton(R.string.terms_agree) { _, _ ->
            consent.record(terms.fingerprint)
            onAgreed(download.voiceModelFileName)
          }
          .setNegativeButton(R.string.terms_cancel, null)
    }
    replaceDialog(builder)
  }

  private fun showError(forVoiceModel: String?) {
    replaceDialog(
        AlertDialog.Builder(activity)
            .setTitle(R.string.terms_title)
            .setMessage(R.string.terms_error)
            .setPositiveButton(R.string.terms_retry) { _, _ -> show(forVoiceModel) }
            .setNegativeButton(R.string.terms_cancel, null)
    )
  }

  private fun replaceDialog(builder: AlertDialog.Builder): AlertDialog {
    dialog?.dismiss()
    return builder.show().also { dialog = it }
  }

  private fun body(text: String) =
      TextView(activity).apply {
        this.text = text
        setTextIsSelectable(true)
      }

  /**
   * A download the user is asked to agree to, of [bytes] for the voice model [voiceModelFileName].
   */
  private class PlannedDownload(val voiceModelFileName: String, val bytes: Long)
}
