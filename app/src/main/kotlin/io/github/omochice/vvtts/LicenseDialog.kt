package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.omochice.vvtts.licenses.LibraryNotice
import io.github.omochice.vvtts.licenses.LicenseBody

/** Shows the licenses of one library over [activity], one dialog at a time. */
internal class LicenseDialog(private val activity: Activity) {
  private var dialog: AlertDialog? = null

  /** Shows the licenses of [notice], replacing the licenses of any library shown before. */
  fun show(notice: LibraryNotice) {
    val documents =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          val gap = activity.dp(24)
          setPadding(gap, activity.dp(8), gap, 0)
          if (notice.licenses.isEmpty()) {
            addView(body(activity.getString(R.string.licenses_unknown)))
          }
          for (license in notice.licenses) {
            addView(activity.headingView(license.name))
            addView(body(describe(license.body)))
          }
        }
    dialog?.dismiss()
    dialog =
        AlertDialog.Builder(activity)
            .setTitle(notice.name)
            .setView(ScrollView(activity).apply { addView(documents) })
            .setPositiveButton(R.string.terms_close, null)
            .show()
  }

  /** Dismisses the dialog if it is showing. */
  fun close() {
    dialog?.dismiss()
  }

  private fun describe(body: LicenseBody): String =
      when (body) {
        is LicenseBody.Text -> body.text
        is LicenseBody.Elsewhere -> activity.getString(R.string.licenses_text_elsewhere, body.url)
        LicenseBody.Missing -> activity.getString(R.string.licenses_text_missing)
      }

  private fun body(text: String) =
      TextView(activity).apply {
        this.text = text
        setTextIsSelectable(true)
      }
}
