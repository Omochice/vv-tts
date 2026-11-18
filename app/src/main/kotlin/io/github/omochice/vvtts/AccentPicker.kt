package io.github.omochice.vvtts

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.omochice.vvtts.dictionary.DictionaryEntry
import io.github.omochice.vvtts.dictionary.ReadingCheck

/**
 * The accent line of the word dialog over [activity]: the current choice, a button to choose, and a
 * note below them.
 *
 * Choosing reads the reading from [reading] and asks [pronouncer] for its choices; a reading that
 * fails validation is handed to [readingProblem] as the string resource saying what is wrong.
 * Results arrive only until [close].
 */
internal class AccentPicker(
    private val activity: Activity,
    private val pronouncer: BackgroundPronouncer,
    private val reading: () -> String,
    private val readingProblem: (Int) -> Unit,
) {
  private val voice = pronouncer.voice()
  private val list = AccentChoiceDialog(activity, pronouncer)

  /** Null while the user has not chosen, which is stored as flat. */
  private var chosen: Int? = null
  private var known: Known? = null

  // Bumped by every change that makes a pending result stale, because a request to the engine
  // cannot be cancelled once it waits for the lock.
  private var generation = 0

  private val label = TextView(activity)
  private val button =
      Button(activity).apply {
        setText(R.string.button_choose)
        setOnClickListener { choose() }
      }
  private val note = TextView(activity)

  /** The view holding the line and its note. */
  val view: View =
      LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(
            LinearLayout(activity).apply {
              orientation = LinearLayout.HORIZONTAL
              gravity = Gravity.CENTER_VERTICAL
              addView(
                  label,
                  LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
              )
              addView(button)
            }
        )
        addView(note)
      }

  /** The accent to store: the one chosen, or flat when none was. */
  val accent: Int
    get() = chosen ?: 0

  init {
    if (voice == null) {
      button.isEnabled = false
      note.setText(R.string.accent_needs_voice)
    } else {
      note.setText(R.string.accent_note)
    }
    render()
  }

  /**
   * Starts from [stored], the accent of an entry being changed, looking up its moras in the
   * background to draw it.
   */
  fun restore(stored: Int) {
    if (stored == 0) return
    chosen = stored
    render()
    val check = DictionaryEntry.readingOf(reading())
    if (check is ReadingCheck.Valid) fetch(check.reading, openList = false)
  }

  /** Forgets the choice, which belongs to the reading before the change. */
  fun readingChanged() {
    generation++
    chosen = null
    known = null
    setBusy(false)
    if (voice != null) note.setText(R.string.accent_note)
    render()
  }

  /** Dismisses the list and drops results still to come. */
  fun close() {
    generation++
    list.close()
  }

  private fun choose() {
    when (val check = DictionaryEntry.readingOf(reading())) {
      is ReadingCheck.Invalid -> readingProblem(check.problem.messageRes())
      is ReadingCheck.Valid -> {
        val choices = known?.takeIf { it.reading == check.reading }?.choices
        if (choices == null) fetch(check.reading, openList = true)
        else openList(check.reading, choices)
      }
    }
  }

  private fun fetch(reading: String, openList: Boolean) {
    val voice = voice ?: return
    val token = ++generation
    setBusy(true)
    pronouncer.run({ it.accentChoices(reading, voice) }) { result ->
      if (token != generation) return@run
      setBusy(false)
      val choices = result.getOrElse {
        if (openList) note.setText(R.string.accent_engine_failed)
        return@run
      }
      known = choices?.let { Known(reading, it) }
      render()
      when {
        !openList -> Unit
        choices == null -> note.setText(R.string.accent_unavailable)
        else -> openList(reading, choices)
      }
    }
  }

  private fun openList(reading: String, choices: AccentChoices) {
    val current = chosen?.takeIf { it <= choices.moras.size }
    list.show(reading, choices, current ?: choices.suggested) {
      chosen = it
      render()
    }
  }

  private fun setBusy(busy: Boolean) {
    button.isEnabled = !busy && voice != null
    button.setText(if (busy) R.string.button_preparing else R.string.button_choose)
  }

  private fun render() {
    val accent = chosen ?: 0
    val moras = known?.choices?.moras
    val shown =
        when {
          accent == 0 -> activity.getString(R.string.accent_flat)
          moras != null && accent <= moras.size -> withFall(moras, accent)
          else -> activity.getString(R.string.accent_position, accent)
        }
    label.text = activity.getString(R.string.accent_line, shown)
  }

  private class Known(val reading: String, val choices: AccentChoices)
}
