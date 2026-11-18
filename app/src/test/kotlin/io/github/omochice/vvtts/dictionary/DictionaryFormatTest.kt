package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals

class DictionaryFormatTest {
  private fun entry(word: String, reading: String) =
      (DictionaryEntry.of(word, reading) as EntryCheck.Valid).entry

  @Test
  fun `each entry is one line of the word and the reading separated by a tab`() {
    assertEquals(
        "nginx\tエンジンエックス\nnode.js\tノードジェイエス\n",
        DictionaryFormat.encode(listOf(entry("nginx", "エンジンエックス"), entry("node.js", "ノードジェイエス"))),
    )
  }

  @Test
  fun `no entries encode to an empty text`() {
    assertEquals("", DictionaryFormat.encode(emptyList()))
  }

  @Test
  fun `encoded entries decode to the same entries`() {
    val entries = listOf(entry("hello world", "ハローワールド"), entry("nginx", "エンジンエックス"))

    assertEquals(entries, DictionaryFormat.decode(DictionaryFormat.encode(entries)))
  }

  @Test
  fun `decoded entries are ordered by word, ignoring the case of ASCII letters`() {
    assertEquals(
        listOf("apple", "Banana", "cherry", "あ"),
        DictionaryFormat.decode("あ\tア\ncherry\tチェリー\nBanana\tバナナ\napple\tアップル\n").map {
          it.word
        },
    )
  }

  @Test
  fun `a line that fails validation is dropped and the others are kept`() {
    val text =
        "nginx\tエンジンエックス\n" + "\tカラ\n" + "why?\tホワイ\n" + "latin\tlatin\n" + "node.js\tノードジェイエス\n"

    assertEquals(listOf("nginx", "node.js"), DictionaryFormat.decode(text).map { it.word })
  }

  @Test
  fun `a line without exactly one tab is dropped`() {
    val text = "nginx エンジンエックス\nnginx\tエンジン\tエックス\nnode.js\tノードジェイエス\n"

    assertEquals(listOf("node.js"), DictionaryFormat.decode(text).map { it.word })
  }

  @Test
  fun `blank lines, a missing final newline and carriage returns are tolerated`() {
    val text = "\n\nnginx\tエンジンエックス\r\n\nnode.js\tノードジェイエス"

    assertEquals(
        listOf(entry("nginx", "エンジンエックス"), entry("node.js", "ノードジェイエス")),
        DictionaryFormat.decode(text),
    )
  }

  @Test
  fun `a hiragana reading in the file is read as katakana`() {
    assertEquals("エンジンエックス", DictionaryFormat.decode("nginx\tえんじんえっくす").single().reading)
  }

  @Test
  fun `of lines whose words differ only in ASCII case, the last one wins`() {
    val decoded = DictionaryFormat.decode("nginx\tエンジンエックス\nNGINX\tンギンクス\n")

    assertEquals(listOf(entry("NGINX", "ンギンクス")), decoded)
  }
}
