package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkLengthTest {
  private fun maxAt(speedScale: Double): Int = maxChunkLength(Prosody(speedScale, pitchScale = 0.0))

  @Test
  fun `normal speed allows 80 code units`() {
    assertEquals(80, maxAt(1.0))
  }

  @Test
  fun `a slower speed allows proportionally fewer code units, rounded down`() {
    assertEquals(49, maxAt(0.62))
  }

  @Test
  fun `a faster speed allows proportionally more code units`() {
    assertEquals(160, maxAt(2.0))
  }

  @Test
  fun `a very slow speed is held at the floor of 20 code units`() {
    assertEquals(20, maxAt(0.01))
  }
}
