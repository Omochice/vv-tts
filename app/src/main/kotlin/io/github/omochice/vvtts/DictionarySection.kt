package io.github.omochice.vvtts

import android.app.Activity
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.github.omochice.vvtts.dictionary.DictionaryEntry
import io.github.omochice.vvtts.dictionary.UserDictionary
import java.io.IOException

/**
 * Lists the words of [dictionary] over [activity], and lets the user add, change and remove them.
 *
 * It talks to the stored dictionary only; the speech engine notices the change on its next
 * synthesis.
 */
internal class DictionarySection(
    private val activity: Activity,
    private val dictionary: UserDictionary,
) {
  private val wordDialog =
      WordDialog(activity) {
        dictionary.add(it)
        refresh()
      }
  private val rows = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  private val status = TextView(activity)

  /** The view holding the heading, the note, the add button and the words. */
  val view: View =
      LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(activity.headingView(activity.getString(R.string.dictionary_heading)))
        addView(TextView(activity).apply { setText(R.string.dictionary_note) })
        addView(
            Button(activity).apply {
              setText(R.string.button_add)
              setOnClickListener { wordDialog.show() }
            },
            LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                .apply { gravity = Gravity.END },
        )
        addView(status)
        addView(rows)
      }

  /** Shows the words as currently stored. */
  fun refresh() {
    rows.removeAllViews()
    val entries =
        try {
          dictionary.entries()
        } catch (_: IOException) {
          show(R.string.dictionary_read_failed)
          return
        }
    if (entries.isEmpty()) show(R.string.dictionary_empty) else status.visibility = View.GONE
    entries.forEach { rows.addView(row(it)) }
  }

  /** Dismisses the word dialog if it is showing. */
  fun close() {
    wordDialog.close()
  }

  private fun show(message: Int) {
    status.setText(message)
    status.visibility = View.VISIBLE
  }

  private fun row(entry: DictionaryEntry): View =
      LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(selectableBackground())
        setOnClickListener { wordDialog.show(entry) }
        addView(
            TextView(activity).apply {
              text = activity.getString(R.string.dictionary_entry, entry.word, entry.reading)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        addView(
            Button(activity).apply {
              setText(R.string.button_remove)
              setOnClickListener { remove(entry) }
            }
        )
      }

  private fun selectableBackground(): Int {
    val value = TypedValue()
    activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
    return value.resourceId
  }

  private fun remove(entry: DictionaryEntry) {
    try {
      dictionary.remove(entry.word)
    } catch (_: IOException) {
      Toast.makeText(activity, R.string.dictionary_save_failed, Toast.LENGTH_LONG).show()
    }
    refresh()
  }
}
