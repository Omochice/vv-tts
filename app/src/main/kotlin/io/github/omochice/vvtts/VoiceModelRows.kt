package io.github.omochice.vvtts

import android.content.Context
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.omochice.vvtts.assets.VoiceModel

/**
 * Lists [voiceModels] with the voices each one holds, offering to download those not installed.
 *
 * @param onDownload called with the voice model whose download button the user pressed.
 */
internal class VoiceModelRows(
    context: Context,
    voiceModels: List<VoiceModel>,
    onDownload: (VoiceModel) -> Unit,
) {
  private val rows = voiceModels.map { Row(context, it, onDownload) }

  /** The view holding every row, in the order of the voice models. */
  val view: View =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        rows.forEach { addView(it.view) }
      }

  /**
   * Shows which voice models [isInstalled] accepts, and enables the download buttons of the others
   * only while [allowsDownload].
   */
  fun update(isInstalled: (voiceModelFileName: String) -> Boolean, allowsDownload: Boolean) {
    for (row in rows) row.update(isInstalled(row.voiceModel.file.fileName), allowsDownload)
  }

  private class Row(
      context: Context,
      val voiceModel: VoiceModel,
      onDownload: (VoiceModel) -> Unit,
  ) {
    private val installed = TextView(context).apply { setText(R.string.voice_model_installed) }
    private val download =
        Button(context).apply {
          setText(R.string.button_download)
          setOnClickListener { onDownload(voiceModel) }
        }

    val view: View =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(0, context.dp(12), 0, 0)
          addView(
              TextView(context).apply {
                text = characterStylesOf(voiceModel.voices).joinToString("\n") { it.label }
              }
          )
          addView(
              LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                      text = Formatter.formatShortFileSize(context, voiceModel.file.sizeBytes)
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                addView(installed)
                addView(download)
              }
          )
        }

    fun update(isInstalled: Boolean, allowsDownload: Boolean) {
      installed.visibility = if (isInstalled) View.VISIBLE else View.GONE
      download.visibility = if (isInstalled) View.GONE else View.VISIBLE
      download.isEnabled = allowsDownload
    }
  }
}
