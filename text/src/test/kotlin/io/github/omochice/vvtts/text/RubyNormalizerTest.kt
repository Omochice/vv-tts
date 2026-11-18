package io.github.omochice.vvtts.text

import kotlin.test.Test
import kotlin.test.assertEquals

class RubyNormalizerTest {
  private fun normalize(text: String): String = RubyNormalizer.normalize(text)

  @Test
  fun `full-width bar keeps only the reading`() {
    assertEquals("かんじを読む。", normalize("｜漢字《かんじ》を読む。"))
  }

  @Test
  fun `half-width bar keeps only the reading`() {
    assertEquals("かんじを読む。", normalize("|漢字《かんじ》を読む。"))
  }

  @Test
  fun `bar base may contain kana and symbols`() {
    assertEquals("ふりがなを読む", normalize("｜Ａ組の友《ふりがな》を読む"))
  }

  @Test
  fun `bar base is not limited to the kanji run`() {
    assertEquals("あいう", normalize("｜お兄様《あいう》"))
  }

  @Test
  fun `double angle brackets after kanji keep only the reading`() {
    assertEquals("かんじを読む。", normalize("漢字《かんじ》を読む。"))
  }

  @Test
  fun `full-width parentheses after kanji keep only the reading`() {
    assertEquals("かんじを読む。", normalize("漢字（かんじ）を読む。"))
  }

  @Test
  fun `half-width parentheses after kanji keep only the reading`() {
    assertEquals("かんじを読む。", normalize("漢字(かんじ)を読む。"))
  }

  @Test
  fun `katakana reading in parentheses is accepted`() {
    assertEquals("カンジを読む", normalize("漢字（カンジ）を読む"))
  }

  @Test
  fun `long vowel mark and middle dot are accepted in a parenthesised reading`() {
    assertEquals("とーきょー・おおさか", normalize("東京大阪（とーきょー・おおさか）"))
  }

  @Test
  fun `reading longer than the base is accepted in parentheses`() {
    assertEquals("ふりがなです", normalize("字（ふりがな）です"))
  }

  @Test
  fun `reading of the same length as the base is accepted in parentheses`() {
    assertEquals("かな", normalize("漢字（かな）"))
  }

  @Test
  fun `double angle brackets without a kanji run stay untouched`() {
    assertEquals("「あ」と《かっこ》で言う", normalize("「あ」と《かっこ》で言う"))
  }

  @Test
  fun `double angle brackets after kana stay untouched`() {
    assertEquals("おにいさま《にいさま》", normalize("おにいさま《にいさま》"))
  }

  @Test
  fun `parenthesised aside with kanji stays untouched`() {
    assertEquals("彼（主人公）は走る", normalize("彼（主人公）は走る"))
  }

  @Test
  fun `parenthesised digits and kanji stay untouched`() {
    assertEquals("東京（2020年）", normalize("東京（2020年）"))
  }

  @Test
  fun `empty parentheses stay untouched`() {
    assertEquals("漢字（）", normalize("漢字（）"))
  }

  @Test
  fun `parenthesised reading shorter than the base stays untouched`() {
    assertEquals("東京大阪（とう）", normalize("東京大阪（とう）"))
  }

  @Test
  fun `parentheses of mixed width stay untouched`() {
    assertEquals("漢字（かんじ)", normalize("漢字（かんじ)"))
  }

  @Test
  fun `parentheses after kana stay untouched`() {
    assertEquals("かんじ（かんじ）", normalize("かんじ（かんじ）"))
  }

  @Test
  fun `empty double angle brackets stay untouched`() {
    assertEquals("漢字《》", normalize("漢字《》"))
  }

  @Test
  fun `empty double angle brackets after a bar stay untouched`() {
    assertEquals("｜漢字《》", normalize("｜漢字《》"))
  }

  @Test
  fun `reading with a line break stays untouched`() {
    assertEquals("漢字《かん\nじ》", normalize("漢字《かん\nじ》"))
  }

  @Test
  fun `reading with another opening bracket stays untouched`() {
    assertEquals("漢字《か《んじ》", normalize("漢字《か《んじ》"))
  }

  @Test
  fun `unclosed double angle bracket stays untouched`() {
    assertEquals("漢字《かんじ", normalize("漢字《かんじ"))
  }

  @Test
  fun `unclosed parenthesis stays untouched`() {
    assertEquals("漢字（かんじ", normalize("漢字（かんじ"))
  }

  @Test
  fun `kana before the kanji run is kept`() {
    assertEquals("おにいさまが来た", normalize("お兄様《にいさま》が来た"))
  }

  @Test
  fun `kana before a parenthesised base is kept`() {
    assertEquals("おにいさま", normalize("お兄様（にいさま）"))
  }

  @Test
  fun `iteration mark belongs to the kanji run`() {
    assertEquals("ときどき", normalize("時々《ときどき》"))
  }

  @Test
  fun `abbreviation marks belong to the kanji run`() {
    assertEquals("いっかげつ", normalize("一ヶ月《いっかげつ》"))
  }

  @Test
  fun `kanji run before a double angle bracket is maximal`() {
    assertEquals("山田ですとうきょうおおさか", normalize("山田です東京大阪《とうきょうおおさか》"))
  }

  @Test
  fun `several annotations in one sentence are all replaced`() {
    assertEquals("かんじとひらがなを読む", normalize("漢字《かんじ》と平仮名《ひらがな》を読む"))
  }

  @Test
  fun `mixed styles in one sentence are all replaced`() {
    assertEquals("あととうきょうでかんじ", normalize("｜A《あ》と東京（とうきょう）で漢字《かんじ》"))
  }

  @Test
  fun `text without ruby is returned unchanged`() {
    assertEquals("吾輩は猫である。", normalize("吾輩は猫である。"))
  }

  @Test
  fun `empty input gives empty output`() {
    assertEquals("", normalize(""))
  }
}
