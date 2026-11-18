package io.github.omochice.vvtts

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView

/** Converts [value] density-independent pixels to whole pixels on this display. */
internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/** Creates a bold line that titles the views below it. */
internal fun Context.headingView(text: String): TextView =
    TextView(this).apply {
      this.text = text
      setTypeface(typeface, Typeface.BOLD)
      setPadding(0, dp(16), 0, dp(4))
    }
