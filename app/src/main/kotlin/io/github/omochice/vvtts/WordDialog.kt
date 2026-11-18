package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import io.github.omochice.vvtts.dictionary.DictionaryEntry
import io.github.omochice.vvtts.dictionary.EntryCheck
import java.io.IOException

/**
 * Asks over [activity] for a word, its reading and its accent, and hands a valid entry to [save].
 *
 * The dialog stays open with a message while the input is invalid or [save] throws [IOException].
 */
internal class WordDialog(
    private val activity: Activity,
    private val pronouncer: BackgroundPronouncer,
    private val save: (DictionaryEntry) -> Unit,
) {
  private var dialog: AlertDialog? = null
  private var picker: AccentPicker? = null

  /**
   * Shows the dialog, replacing one already showing: empty to add a word, or filled with [entry] to
   * change its reading and accent.
   */
  fun show(entry: DictionaryEntry? = null) {
    val word = field(R.string.dictionary_word)
    val reading = field(R.string.dictionary_reading)
    val message =
        TextView(activity).apply {
          visibility = View.GONE
          setTextColor(activity.getColor(android.R.color.holo_red_dark))
        }
    val accent =
        AccentPicker(activity, pronouncer, { reading.text.toString() }) {
          message.setText(it)
          message.visibility = View.VISIBLE
        }
    if (entry != null) {
      word.setText(entry.word)
      // Locked, because the dictionary replaces an entry by its word, so a changed word would add
      // a second entry and leave the old one behind.
      word.isEnabled = false
      reading.setText(entry.reading)
    }
    // Added after filling in the entry, whose own text would otherwise reset its accent.
    reading.addTextChangedListener(onChange(accent::readingChanged))
    entry?.let { accent.restore(it.accent) }
    val form =
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL
          val gap = activity.dp(24)
          setPadding(gap, activity.dp(8), gap, 0)
          addView(word)
          addView(reading)
          addView(accent.view)
          addView(message)
        }
    close()
    val shown =
        AlertDialog.Builder(activity)
            .setTitle(
                if (entry == null) R.string.dictionary_add_title else R.string.dictionary_edit_title
            )
            .setView(form)
            // The listener is set on the button after show(), because one passed here always
            // dismisses the dialog, even when the input has to be fixed.
            .setPositiveButton(
                if (entry == null) R.string.button_add else R.string.button_save,
                null,
            )
            .setNegativeButton(R.string.button_cancel, null)
            .setOnDismissListener { accent.close() }
            .show()
    shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
      val problem = submit(word.text.toString(), reading.text.toString(), accent.accent)
      if (problem == null) {
        shown.dismiss()
      } else {
        message.setText(problem)
        message.visibility = View.VISIBLE
      }
    }
    dialog = shown
    picker = accent
  }

  /** Dismisses the dialog if it is showing, together with the accent list it opened. */
  fun close() {
    // Closed here rather than only from the dismiss listener, which Android runs a message later,
    // after an activity being destroyed has already lost its window.
    picker?.close()
    dialog?.dismiss()
  }

  /** Returns null once saved, otherwise the string resource saying what went wrong. */
  private fun submit(word: String, reading: String, accent: Int): Int? =
      when (val check = DictionaryEntry.of(word, reading, accent)) {
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

/** Returns a watcher that runs [action] after every change of the text. */
internal fun onChange(action: () -> Unit): TextWatcher =
    object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

      override fun afterTextChanged(s: Editable?) = action()
    }
