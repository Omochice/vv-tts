package io.github.omochice.vvtts

import kotlin.test.Test
import kotlin.test.assertEquals

class AccentLabelTest {
  private val moras = listOf("エ", "ン", "ジ", "ン", "エ", "ッ", "ク", "ス")

  @Test
  fun `the fall is drawn after the moras before it`() {
    assertEquals("エンジンエ＼ックス", withFall(moras, 5))
    assertEquals("エ＼ンジンエックス", withFall(moras, 1))
  }

  @Test
  fun `a fall after the last mora is drawn at the end`() {
    assertEquals("エンジンエックス＼", withFall(moras, 8))
  }

  @Test
  fun `moras of more than one character stay whole`() {
    assertEquals("キュ＼ーブ", withFall(listOf("キュ", "ー", "ブ"), 1))
  }
}
