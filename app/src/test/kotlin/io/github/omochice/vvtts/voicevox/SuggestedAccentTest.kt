package io.github.omochice.vvtts.voicevox

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestedAccentTest {
  @Test
  fun `the analyzed accent of a single phrase of the same moras is suggested`() {
    assertEquals(5, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 8, accent = 5)), 8))
    assertEquals(1, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 4, accent = 1)), 4))
  }

  @Test
  fun `a fall after the last mora is suggested as flat`() {
    assertEquals(0, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 8, accent = 8)), 8))
  }

  @Test
  fun `a phrase of another mora count suggests flat`() {
    assertEquals(0, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 12, accent = 5)), 11))
  }

  @Test
  fun `more than one phrase or none suggests flat`() {
    val two =
        listOf(AnalyzedPhrase(moraCount = 4, accent = 1), AnalyzedPhrase(moraCount = 4, accent = 2))
    assertEquals(0, suggestedAccent(two, 8))
    assertEquals(0, suggestedAccent(emptyList(), 8))
  }

  @Test
  fun `an accent outside the moras suggests flat`() {
    assertEquals(0, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 8, accent = 0)), 8))
    assertEquals(0, suggestedAccent(listOf(AnalyzedPhrase(moraCount = 8, accent = 9)), 8))
  }
}
