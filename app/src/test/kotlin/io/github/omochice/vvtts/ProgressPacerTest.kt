package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressPacerTest {
  private var nowMillis = 50_000L
  private val pacer = ProgressPacer(minIntervalMillis = 1000) { nowMillis }

  @Test
  fun `the first percentage is shown`() {
    assertTrue(pacer.shouldShow(0))
  }

  @Test
  fun `a percentage already shown is not shown again however long ago it was`() {
    pacer.shouldShow(40)
    nowMillis += 5000

    assertFalse(pacer.shouldShow(40))
  }

  @Test
  fun `a new percentage sooner than the interval after the last one shown is held back`() {
    pacer.shouldShow(40)
    nowMillis += 999

    assertFalse(pacer.shouldShow(41))
  }

  @Test
  fun `a new percentage is shown once the interval has passed`() {
    pacer.shouldShow(40)
    nowMillis += 1000

    assertTrue(pacer.shouldShow(41))
  }

  @Test
  fun `a percentage held back is shown when it comes again after the interval`() {
    pacer.shouldShow(40)
    nowMillis += 500
    pacer.shouldShow(41)
    nowMillis += 500

    assertTrue(pacer.shouldShow(41))
  }

  @Test
  fun `the interval counts from the last percentage shown, not from one held back`() {
    pacer.shouldShow(40)
    nowMillis += 600
    pacer.shouldShow(41)
    nowMillis += 600

    assertTrue(pacer.shouldShow(42))
  }
}
