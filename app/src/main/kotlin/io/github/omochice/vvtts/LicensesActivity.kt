package io.github.omochice.vvtts

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.github.omochice.vvtts.licenses.LibraryNotice
import io.github.omochice.vvtts.licenses.libraryNoticesOf

/** Lists the libraries the APK carries, and shows the licenses of the one the user taps. */
class LicensesActivity : Activity() {
  private lateinit var dialog: LicenseDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dialog = LicenseDialog(this)
    val notices =
        libraryNoticesOf(
            resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        )
    val gap = dp(16)
    val empty =
        TextView(this).apply {
          setText(R.string.licenses_load_failed)
          setPadding(gap, gap, gap, gap)
        }
    val list =
        ListView(this).apply {
          clipToPadding = false
          adapter = NoticeAdapter(this@LicensesActivity, notices)
          setOnItemClickListener { _, _, position, _ -> dialog.show(notices[position]) }
          setOnApplyWindowInsetsListener { view, insets ->
            val bars =
                insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsets.CONSUMED
          }
        }
    setContentView(
        FrameLayout(this).apply {
          addView(list)
          addView(empty)
        }
    )
    list.emptyView = empty
  }

  override fun onDestroy() {
    dialog.close()
    super.onDestroy()
  }

  private class NoticeAdapter(
      private val context: Context,
      private val notices: List<LibraryNotice>,
  ) : BaseAdapter() {
    override fun getCount(): Int = notices.size

    override fun getItem(position: Int): LibraryNotice = notices[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
      val row = convertView?.tag as? Row ?: Row(context)
      row.show(notices[position])
      return row.view
    }
  }

  private class Row(private val context: Context) {
    private val name = TextView(context).apply { textSize = 16f }
    private val detail = TextView(context)

    val view: View =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          val gap = context.dp(16)
          setPadding(gap, context.dp(12), gap, context.dp(12))
          addView(name)
          addView(detail)
          tag = this@Row
        }

    fun show(notice: LibraryNotice) {
      name.text = notice.name
      val licenses =
          notice.licenseNames.joinToString("、").ifEmpty {
            context.getString(R.string.licenses_unknown)
          }
      detail.text =
          notice.version?.let { context.getString(R.string.licenses_row_detail, it, licenses) }
              ?: licenses
    }
  }
}
