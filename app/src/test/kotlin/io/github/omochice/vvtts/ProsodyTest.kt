package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertEquals

class ProsodyTest {
  private val tolerance = 1e-9

  @Test
  fun `normal rate and pitch map to the neutral scales`() {
    val prosody = Prosody.fromAndroid(rate = 100, pitch = 100)

    assertEquals(1.0, prosody.speedScale, tolerance)
    assertEquals(0.0, prosody.pitchScale, tolerance)
  }

  @Test
  fun `a faster rate raises the speed scale proportionally`() {
    assertEquals(2.0, Prosody.fromAndroid(rate = 200, pitch = 100).speedScale, tolerance)
  }

  @Test
  fun `a slower rate lowers the speed scale proportionally`() {
    assertEquals(0.5, Prosody.fromAndroid(rate = 50, pitch = 100).speedScale, tolerance)
  }

  @Test
  fun `a higher pitch raises the pitch scale by 0_15 per 100 points`() {
    assertEquals(0.15, Prosody.fromAndroid(rate = 100, pitch = 200).pitchScale, tolerance)
  }

  @Test
  fun `a lower pitch lowers the pitch scale by 0_15 per 100 points`() {
    assertEquals(-0.075, Prosody.fromAndroid(rate = 100, pitch = 50).pitchScale, tolerance)
  }
}
