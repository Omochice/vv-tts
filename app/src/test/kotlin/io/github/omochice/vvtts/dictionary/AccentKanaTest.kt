package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccentKanaTest {
  @Test
  fun `each long vowel mark becomes the vowel of the mora before it`() {
    assertEquals("キュウブコントロオル", kanaWithoutLongVowels("キューブコントロール"))
    assertEquals("ティイウォ", kanaWithoutLongVowels("ティーウォ"))
  }

  @Test
  fun `a reading without the long vowel mark is kept as it is`() {
    assertEquals("エンジンエックス", kanaWithoutLongVowels("エンジンエックス"))
  }

  @Test
  fun `the long vowel mark takes the vowel of each of the five rows`() {
    assertEquals("カアキイクウケエコオ", kanaWithoutLongVowels("カーキークーケーコー"))
  }

  @Test
  fun `after a small kana the long vowel mark takes the vowel of the small kana`() {
    assertEquals("ファアジェエショオクヮア", kanaWithoutLongVowels("ファージェーショークヮー"))
  }

  @Test
  fun `the voiced u and the old kana wi, we and wo have vowels too`() {
    assertEquals("ヴウヰイヱエヲオ", kanaWithoutLongVowels("ヴーヰーヱーヲー"))
  }

  @Test
  fun `every kana the reading may hold has a vowel except the sokuon and the moraic n`() {
    for (kana in ('ァ'..'ヴ').filter { it != 'ッ' && it != 'ン' }) {
      assertEquals(2, kanaWithoutLongVowels("${kana}ー")?.length, kana.toString())
    }
  }

  @Test
  fun `a doubled long vowel mark repeats the same vowel`() {
    assertEquals("ロオオ", kanaWithoutLongVowels("ローー"))
  }

  @Test
  fun `a long vowel mark with no vowel before it cannot be converted`() {
    assertNull(kanaWithoutLongVowels("ーエ"))
    assertNull(kanaWithoutLongVowels("アンー"))
    assertNull(kanaWithoutLongVowels("アッー"))
  }

  @Test
  fun `the mark goes after the moras before the fall`() {
    assertEquals("エンジンエ'ックス", markAccent("エンジンエックス", List(8) { 1 }, 5))
    assertEquals("ファ'ッション", markAccent("ファッション", listOf(2, 1, 2, 1), 1))
    assertEquals("ヴァ'イオリン", markAccent("ヴァイオリン", listOf(2, 1, 1, 1, 1), 1))
  }

  @Test
  fun `flat and a fall after the last mora both put the mark at the end`() {
    assertEquals("エンジンエックス'", markAccent("エンジンエックス", List(8) { 1 }, 0))
    assertEquals("エンジンエックス'", markAccent("エンジンエックス", List(8) { 1 }, 8))
  }

  @Test
  fun `an accent outside the moras cannot be marked`() {
    assertNull(markAccent("エンジンエックス", List(8) { 1 }, -1))
    assertNull(markAccent("エンジンエックス", List(8) { 1 }, 9))
  }

  @Test
  fun `moras that do not cover the kana exactly cannot be marked`() {
    assertNull(markAccent("エンジンエックス", List(7) { 1 }, 1))
    assertNull(markAccent("アイ", listOf(1, 0, 1), 1))
  }

  @Test
  fun `the reading is cut into pieces of the given lengths`() {
    assertEquals(
        listOf("キュ", "ー", "ブ", "コ", "ン", "ト", "ロ", "ー", "ル"),
        moraPieces("キューブコントロール", listOf(2, 1, 1, 1, 1, 1, 1, 1, 1)),
    )
  }

  @Test
  fun `lengths that do not cover the reading exactly cut nothing`() {
    assertNull(moraPieces("キュー", listOf(2)))
    assertNull(moraPieces("キュー", listOf(2, 2)))
    assertNull(moraPieces("キュー", listOf(2, 0, 1)))
  }
}
