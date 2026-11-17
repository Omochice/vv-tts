package io.github.omochice.vvtts.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SentenceSplitterTest {
  private fun split(text: String, maxBracketContentLength: Int = 100): List<String> =
      SentenceSplitter(maxBracketContentLength).split(text)

  @Test
  fun `text without any delimiter is returned as a single chunk`() {
    assertEquals(listOf("吾輩は猫である"), split("吾輩は猫である"))
  }

  @Test
  fun `full stop ends a chunk and stays at its end`() {
    assertEquals(
        listOf("吾輩は猫である。", "名前はまだ無い。"),
        split("吾輩は猫である。名前はまだ無い。"),
    )
  }

  @Test
  fun `exclamation and question marks end a chunk and stay at its end`() {
    assertEquals(
        listOf("どこで生れたか？", "とんと見当がつかぬ！", "何でも薄暗い所であった。"),
        split("どこで生れたか？とんと見当がつかぬ！何でも薄暗い所であった。"),
    )
  }

  @Test
  fun `newline ends a chunk and is not kept in it`() {
    assertEquals(listOf("第一章", "吾輩は猫である。"), split("第一章\n吾輩は猫である。"))
  }

  @Test
  fun `blank lines and trailing newlines produce no empty chunk`() {
    assertEquals(
        listOf("吾輩は猫である。", "名前はまだ無い。"),
        split("\n吾輩は猫である。\n\n \n名前はまだ無い。\n"),
    )
  }

  @Test
  fun `input with nothing but whitespace produces no chunk`() {
    assertEquals(emptyList(), split(" \n\n"))
  }

  @Test
  fun `full-width space used as paragraph indentation is trimmed from the chunk`() {
    assertEquals(
        listOf("吾輩は猫である。", "名前はまだ無い。"),
        split("　吾輩は猫である。\n　名前はまだ無い。"),
    )
  }

  @Test
  fun `CRLF line ending ends a chunk like a bare newline`() {
    assertEquals(listOf("第一章", "吾輩は猫である。"), split("第一章\r\n吾輩は猫である。"))
  }

  @Test
  fun `reading comma does not end a chunk`() {
    assertEquals(
        listOf("しかもあとで聞くと、それは書生という人間中で一番獰悪な種族であったそうだ。"),
        split("しかもあとで聞くと、それは書生という人間中で一番獰悪な種族であったそうだ。"),
    )
  }

  @Test
  fun `consecutive delimiters stay together at the end of one chunk`() {
    assertEquals(
        listOf("なんだって！？", "そんな馬鹿な！！", "まさか……。"),
        split("なんだって！？そんな馬鹿な！！まさか……。"),
    )
  }

  @Test
  fun `half-width exclamation and question marks end a chunk like full-width ones`() {
    assertEquals(
        listOf("本当か?", "嘘だろ!?", "信じられない。"),
        split("本当か?嘘だろ!?信じられない。"),
    )
  }

  @Test
  fun `delimiters inside a short corner-bracket quote do not end a chunk`() {
    assertEquals(
        listOf("「おい、待て。どこへ行く？」と彼は叫んだ。", "返事はなかった。"),
        split("「おい、待て。どこへ行く？」と彼は叫んだ。返事はなかった。"),
    )
  }

  @Test
  fun `double corner brackets and full- and half-width parentheses also keep their content whole`() {
    for ((open, close) in listOf("『" to "』", "（" to "）", "(" to ")")) {
      val text = "彼は${open}もう遅い。帰ろう。${close}と心の中で呟いた。それから歩き出した。"
      assertEquals(
          listOf("彼は${open}もう遅い。帰ろう。${close}と心の中で呟いた。", "それから歩き出した。"),
          split(text),
          "pair $open$close",
      )
    }
  }

  @Test
  fun `opening bracket never closed in the input does not stop splitting`() {
    assertEquals(
        listOf("彼は振り返った。", "「待ってくれ。", "まだ話は終わっていない。", "君に頼みが"),
        split("彼は振り返った。「待ってくれ。まだ話は終わっていない。君に頼みが", 100),
    )
  }

  @Test
  fun `closing bracket with no opener is kept as an ordinary character`() {
    assertEquals(
        listOf("それでいい」と彼は頷いた。", "外は雨だった。"),
        split("それでいい」と彼は頷いた。外は雨だった。"),
    )
  }

  @Test
  fun `delimiters inside a bracket whose content is exactly the threshold do not end a chunk`() {
    val content = "もう遅い。帰ろう"
    assertEquals(
        listOf("「もう遅い。帰ろう」と彼は言った。"),
        split("「${content}」と彼は言った。", content.length),
    )
  }

  @Test
  fun `delimiters inside a bracket whose content exceeds the threshold end a chunk`() {
    val content = "もう遅い。帰ろう"
    assertEquals(
        listOf("「もう遅い。", "帰ろう」と彼は言った。"),
        split("「${content}」と彼は言った。", content.length - 1),
    )
  }

  @Test
  fun `closing bracket right after a full stop attaches to the preceding chunk`() {
    assertEquals(
        listOf("「もう遅い。", "帰ろう。」", "と彼は言った。"),
        split("「もう遅い。帰ろう。」と彼は言った。", 3),
    )
    assertEquals(
        listOf("それで終わりだ。」", "翌朝、彼は町を出た。"),
        split("それで終わりだ。」\n翌朝、彼は町を出た。"),
    )
  }

  @Test
  fun `closing brackets after an exclamation mark attach to the preceding chunk`() {
    assertEquals(
        listOf("「彼は叫んだ。", "『もう駄目だ！』」", "夜が明けた。"),
        split("「彼は叫んだ。『もう駄目だ！』」\n夜が明けた。", 2),
    )
  }

  @Test
  fun `several closing brackets after a full stop all attach to the preceding chunk`() {
    assertEquals(
        listOf("「彼は言った。", "『僕は見た。", "（あれは夢だ。）』」", "誰も答えなかった。"),
        split("「彼は言った。『僕は見た。（あれは夢だ。）』」誰も答えなかった。", 2),
    )
  }

  @Test
  fun `short inner quote stays whole while its long outer quote is split`() {
    val inner = "明日は晴れる。きっと晴れる。"
    assertEquals(
        listOf("「僕は思った。", "『明日は晴れる。きっと晴れる。』そう信じていた。」"),
        split("「僕は思った。『${inner}』そう信じていた。」", inner.length),
    )
  }

  @Test
  fun `long inner quote inside a long outer quote is split as well`() {
    assertEquals(
        listOf("「僕は思った。", "『明日は晴れる。", "きっと晴れる。』", "そう信じていた。」"),
        split("「僕は思った。『明日は晴れる。きっと晴れる。』そう信じていた。」", 3),
    )
  }

  @Test
  fun `closing bracket pairs with its own kind of opener even past an unclosed inner opener`() {
    assertEquals(
        listOf("「もう帰る。（本当は帰りたくない」と彼女は言った。"),
        split("「もう帰る。（本当は帰りたくない」と彼女は言った。"),
    )
  }

  @Test
  fun `adjacent short quotes on one line form a single chunk`() {
    assertEquals(
        listOf("「おはよう。」「おはよう。今日は早いね。」", "二人は笑った。"),
        split("「おはよう。」「おはよう。今日は早いね。」\n二人は笑った。"),
    )
  }

  @Test
  fun `newline inside a short quote does not end a chunk`() {
    assertEquals(
        listOf("「待って\nくれ。」と彼は言った。"),
        split("「待って\nくれ。」と彼は言った。"),
    )
  }

  @Test
  fun `chunk made of nothing but brackets is dropped`() {
    assertEquals(listOf("翌朝、彼は町を出た。"), split("」\n翌朝、彼は町を出た。"))
    assertEquals(listOf("彼は振り返った。"), split("彼は振り返った。「"))
  }

  @Test
  fun `stray closing bracket on the line after a full stop attaches across the newline`() {
    assertEquals(
        listOf("それで終わりだ。\n」", "翌朝、彼は町を出た。"),
        split("それで終わりだ。\n」翌朝、彼は町を出た。"),
    )
  }

  @Test
  fun `brackets outside the supported pairs do not keep their content whole`() {
    assertEquals(
        listOf("【注意。", "危険】と書かれていた。"),
        split("【注意。危険】と書かれていた。"),
    )
  }

  @Test
  fun `negative bracket content threshold is rejected`() {
    assertFailsWith<IllegalArgumentException> { SentenceSplitter(-1) }
  }

  @Test
  fun `splitting a passage without bracket-only fragments loses nothing but whitespace`() {
    val text =
        "　吾輩は猫である。名前はまだ無い。\n\n" +
            "「どこで生れたか？とんと見当がつかぬ！」と彼は言った。」\r\n" +
            "『何でも薄暗い（じめじめした。）所で』ニャーニャー泣いていた……。「記憶は"
    fun visible(s: String) = s.filterNot { it.isWhitespace() }
    for (threshold in listOf(0, 5, 100)) {
      assertEquals(
          visible(text),
          visible(split(text, threshold).joinToString("")),
          "threshold $threshold",
      )
    }
  }
}
