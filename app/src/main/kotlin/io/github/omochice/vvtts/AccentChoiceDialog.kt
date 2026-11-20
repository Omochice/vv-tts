package io.github.omochice.vvtts

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.widget.Button
import android.widget.Toast

/**
 * Lets the user pick, over [activity], one of the positions a reading's pitch can fall at, hearing
 * each through [pronouncer] before deciding.
 */
internal class AccentChoiceDialog(
    private val activity: Activity,
    private val pronouncer: BackgroundPronouncer,
) {
  private var dialog: AlertDialog? = null
  private val player = PreviewPlayer()

  // Bumped whenever a preview still being synthesized should no longer play, because a request to
  // the engine cannot be cancelled once it waits for the lock.
  private var generation = 0

  /**
   * Shows flat followed by each position of [choices] for [reading], with [preselected] selected,
   * and hands the accent picked to [choose].
   */
  fun show(reading: String, choices: AccentChoices, preselected: Int, choose: (Int) -> Unit) {
    // The index of each item is its accent, flat being 0.
    val items =
        listOf(activity.getString(R.string.accent_flat)) +
            (1..choices.moras.size).map { withFall(choices.moras, it) }
    var selected = preselected
    var listen: Button? = null
    close()
    val shown =
        AlertDialog.Builder(activity)
            .setTitle(R.string.accent_choose_title)
            .setSingleChoiceItems(items.toTypedArray(), selected) { _, which ->
              selected = which
              silence()
              listen?.let(::setIdle)
            }
            .setPositiveButton(R.string.button_decide) { _, _ -> choose(selected) }
            .setNegativeButton(R.string.button_cancel, null)
            // The listener is set on the button after show(), because one passed here dismisses
            // the dialog, and the user listens to several positions before deciding.
            .setNeutralButton(R.string.button_listen, null)
            .setOnDismissListener { silence() }
            .show()
    val button = shown.getButton(AlertDialog.BUTTON_NEUTRAL)
    button.setOnClickListener { preview(reading, selected, button) }
    listen = button
    dialog = shown
  }

  /** Dismisses the dialog if it is showing, which stops its sound. */
  fun close() {
    // Silenced here as well as from the dismiss listener, which Android runs a message later.
    silence()
    dialog?.dismiss()
  }

  private fun preview(reading: String, accent: Int, button: Button) {
    val voice = pronouncer.voice() ?: return
    silence()
    val token = generation
    button.isEnabled = false
    button.setText(R.string.button_preparing)
    pronouncer.run({ it.speak(reading, accent, voice) }) { result ->
      if (token != generation || dialog?.isShowing != true) return@run
      setIdle(button)
      result.fold(::play) { report() }
    }
  }

  // The track can fail to be created when the system runs short of audio resources,
  // which is no reason to end the settings screen.
  @Suppress("TooGenericExceptionCaught")
  private fun play(pcm: ByteArray) {
    try {
      player.play(pcm)
    } catch (e: RuntimeException) {
      Log.w(TAG, "cannot play the preview: ${e.javaClass.name}")
      report()
    }
  }

  private fun report() {
    Toast.makeText(activity, R.string.preview_failed, Toast.LENGTH_SHORT).show()
  }

  private fun silence() {
    generation++
    player.stop()
  }

  private fun setIdle(button: Button) {
    button.isEnabled = true
    button.setText(R.string.button_listen)
  }
}
