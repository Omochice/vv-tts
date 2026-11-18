package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog

/** Lets the user pick, over [activity], one of the positions a reading's pitch can fall at. */
internal class AccentChoiceDialog(private val activity: Activity) {
  private var dialog: AlertDialog? = null

  /**
   * Shows flat followed by each position of [choices], with [preselected] selected, and hands the
   * accent picked to [choose].
   */
  fun show(choices: AccentChoices, preselected: Int, choose: (Int) -> Unit) {
    // The index of each item is its accent, flat being 0.
    val items =
        listOf(activity.getString(R.string.accent_flat)) +
            (1..choices.moras.size).map { withFall(choices.moras, it) }
    var selected = preselected
    dialog?.dismiss()
    dialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.accent_choose_title)
            .setSingleChoiceItems(items.toTypedArray(), selected) { _, which -> selected = which }
            .setPositiveButton(R.string.button_decide) { _, _ -> choose(selected) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
  }

  /** Dismisses the dialog if it is showing. */
  fun close() {
    dialog?.dismiss()
  }
}
