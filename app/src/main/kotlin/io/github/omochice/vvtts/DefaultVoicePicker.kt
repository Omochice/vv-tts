package io.github.omochice.vvtts

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

/** Lets the user choose, among the installed voices, the one clients get unless they pick one. */
internal class DefaultVoicePicker(private val context: Context) {
  private val preferred = PreferredVoiceStore(context)
  private var voices: List<SpeakableVoice> = emptyList()

  private val spinner =
      Spinner(context).apply {
        onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
              override fun onItemSelected(
                  parent: AdapterView<*>?,
                  view: View?,
                  position: Int,
                  id: Long,
              ) {
                choose(voices[position])
              }

              override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
      }

  /**
   * The view holding the heading, the choice and the note on when it takes effect; hidden until
   * [show] is given a voice.
   */
  val view: View =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        addView(context.headingView(context.getString(R.string.default_voice_heading)))
        addView(spinner)
        addView(TextView(context).apply { setText(R.string.default_voice_note) })
      }

  /**
   * Offers [installed], selecting the default voice among them; hidden while [installed] is empty.
   */
  fun show(installed: List<SpeakableVoice>) {
    // Left alone when nothing changed, because a new adapter closes a dropdown the user has open
    // while a download reports progress.
    if (installed == voices) return
    voices = installed
    view.visibility = if (installed.isEmpty()) View.GONE else View.VISIBLE
    spinner.adapter =
        ArrayAdapter(context, android.R.layout.simple_spinner_item, installed.map { it.name })
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    currentDefault()?.let { spinner.setSelection(installed.indexOf(it)) }
  }

  private fun currentDefault(): SpeakableVoice? = preferred.defaultAmong(voices)

  private fun choose(voice: SpeakableVoice) {
    // Not stored when it is already the default, because the spinner also reports the selection
    // show() makes, and storing a fallback would pin it once other voice models are installed.
    if (voice != currentDefault()) preferred.set(voice.name)
  }
}
