package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertEquals

class PercentTest {
  @Test
  fun `the percentage is rounded down to a whole number`() {
    assertEquals(33, wholePercent(1, 3))
    assertEquals(99, wholePercent(999, 1000))
  }

  @Test
  fun `all bytes done is 100 percent`() {
    assertEquals(100, wholePercent(1000, 1000))
  }

  @Test
  fun `nothing done is 0 percent`() {
    assertEquals(0, wholePercent(0, 1000))
  }

  @Test
  fun `an unknown total is 0 percent`() {
    assertEquals(0, wholePercent(0, 0))
    assertEquals(0, wholePercent(5, 0))
  }

  @Test
  fun `totals beyond the range of an int do not overflow`() {
    assertEquals(50, wholePercent(5_000_000_000, 10_000_000_000))
  }
}
