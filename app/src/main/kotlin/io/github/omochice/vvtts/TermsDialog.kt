package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.text.format.Formatter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.omochice.vvtts.assets.AssetCatalog
import io.github.omochice.vvtts.assets.AssetInstaller
import io.github.omochice.vvtts.assets.Terms

/**
 * Shows the terms of the assets in [catalog] over [activity], and asks the user to agree to them
 * before a download.
 *
 * @param onAgreed called with the voice model the user may now download.
 */
internal class TermsDialog(
    private val activity: Activity,
    private val catalog: AssetCatalog,
    private val installer: AssetInstaller,
    private val onAgreed: (voiceModelFileName: String) -> Unit,
) {
  private val consent = ConsentStore(activity)
  private val terms by lazy { Terms.load(catalog) }
  private val fingerprint by lazy { consentFingerprint(terms) }
  private var dialog: AlertDialog? = null

  /**
   * Lets the download of the voice model named [voiceModelFileName] go ahead, at once when the user
   * already agreed to the current terms, otherwise once they agree.
   */
  fun requestConsent(voiceModelFileName: String) {
    if (consent.isGiven(fingerprint)) {
      onAgreed(voiceModelFileName)
    } else {
      show(forVoiceModel = voiceModelFileName)
    }
  }

  /**
   * Shows the terms, asking for consent to download the voice model named [forVoiceModel] unless it
   * is null.
   */
  fun show(forVoiceModel: String?) {
    val bytes = forVoiceModel?.let(installer::bytesToDownload)
    val documents =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          val gap = activity.dp(24)
          setPadding(gap, activity.dp(8), gap, 0)
          if (bytes != null) {
            addView(
                body(
                    activity.getString(
                        R.string.terms_download_size,
                        Formatter.formatShortFileSize(activity, bytes),
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
    if (forVoiceModel == null) {
      builder.setPositiveButton(R.string.terms_close, null)
    } else {
      builder
          .setPositiveButton(R.string.terms_agree) { _, _ ->
            consent.record(fingerprint)
            onAgreed(forVoiceModel)
          }
          .setNegativeButton(R.string.terms_cancel, null)
    }
    dialog?.dismiss()
    dialog = builder.show()
  }

  /** Dismisses the dialog if it is showing. */
  fun close() {
    dialog?.dismiss()
  }

  private fun body(text: String) =
      TextView(activity).apply {
        this.text = text
        setTextIsSelectable(true)
      }
}
