package io.github.omochice.vvtts

/**
 * Picks the progress percentages worth showing, so that one is shown at most every
 * [minIntervalMillis] milliseconds.
 *
 * @param now returns the current time in milliseconds on a clock that never goes back.
 */
internal class ProgressPacer(private val minIntervalMillis: Long, private val now: () -> Long) {
  private var shownPercent: Int? = null
  private var shownAtMillis = 0L

  /** Returns whether to show [percent] now, taking it as shown when it does. */
  fun shouldShow(percent: Int): Boolean {
    val nowMillis = now()
    val tooSoon = shownPercent != null && nowMillis - shownAtMillis < minIntervalMillis
    if (percent == shownPercent || tooSoon) return false
    shownPercent = percent
    shownAtMillis = nowMillis
    return true
  }
}
