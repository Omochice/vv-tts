package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import io.github.omochice.vvtts.dictionary.DictionaryEntry
import io.github.omochice.vvtts.dictionary.EntryCheck
import java.io.IOException

/**
 * Asks over [activity] for a word and its reading, and hands a valid pair to [save].
 *
 * The dialog stays open with a message while the input is invalid or [save] throws [IOException].
 */
internal class WordDialog(
    private val activity: Activity,
    private val save: (DictionaryEntry) -> Unit,
) {
  private var dialog: AlertDialog? = null

  /** Shows an empty dialog, replacing one already showing. */
  fun show() {
    val word = field(R.string.dictionary_word)
    val reading = field(R.string.dictionary_reading)
    val message =
        TextView(activity).apply {
          visibility = View.GONE
          setTextColor(activity.getColor(android.R.color.holo_red_dark))
        }
    val form =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          val gap = activity.dp(24)
          setPadding(gap, activity.dp(8), gap, 0)
          addView(word)
          addView(reading)
          addView(message)
        }
    dialog?.dismiss()
    val shown =
        AlertDialog.Builder(activity)
            .setTitle(R.string.dictionary_add_title)
            .setView(form)
            // The listener is set on the button after show(), because one passed here always
            // dismisses the dialog, even when the input has to be fixed.
            .setPositiveButton(R.string.button_add, null)
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
      val problem = submit(word.text.toString(), reading.text.toString())
      if (problem == null) {
        shown.dismiss()
      } else {
        message.setText(problem)
        message.visibility = View.VISIBLE
      }
    }
    dialog = shown
  }

  /** Dismisses the dialog if it is showing. */
  fun close() {
    dialog?.dismiss()
  }

  /** Returns null once saved, otherwise the string resource saying what went wrong. */
  private fun submit(word: String, reading: String): Int? =
      when (val check = DictionaryEntry.of(word, reading)) {
        is EntryCheck.Invalid -> check.problem.messageRes()
        is EntryCheck.Valid ->
            try {
              save(check.entry)
              null
            } catch (_: IOException) {
              R.string.dictionary_save_failed
            }
      }

  private fun field(hint: Int) =
      EditText(activity).apply {
        setHint(hint)
        inputType = InputType.TYPE_CLASS_TEXT
        isSingleLine = true
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
      }
}
