package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals

class DictionaryFormatTest {
  private fun entry(word: String, reading: String, accent: Int = 0) =
      (DictionaryEntry.of(word, reading, accent) as EntryCheck.Valid).entry

  @Test
  fun `each entry is one line of the word, the reading and the accent separated by tabs`() {
    assertEquals(
        "nginx\tエンジンエックス\t5\nnode.js\tノードジェイエス\t0\n",
        DictionaryFormat.encode(
            listOf(entry("nginx", "エンジンエックス", 5), entry("node.js", "ノードジェイエス"))
        ),
    )
  }

  @Test
  fun `no entries encode to an empty text`() {
    assertEquals("", DictionaryFormat.encode(emptyList()))
  }

  @Test
  fun `encoded entries decode to the same entries`() {
    val entries = listOf(entry("hello world", "ハローワールド"), entry("nginx", "エンジンエックス", 5))

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
  fun `a line without a tab or with more than two is dropped`() {
    val text = "nginx エンジンエックス\nnginx\tエンジン\t5\tエックス\nnode.js\tノードジェイエス\n"

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
  fun `a line of only the word and the reading, as files without accents have, is flat`() {
    assertEquals(entry("nginx", "エンジンエックス"), DictionaryFormat.decode("nginx\tエンジンエックス\n").single())
  }

  @Test
  fun `a line with an accent decodes to an entry with that accent`() {
    assertEquals(5, DictionaryFormat.decode("nginx\tエンジンエックス\t5\n").single().accent)
  }

  @Test
  fun `a carriage return after the accent is tolerated`() {
    assertEquals(5, DictionaryFormat.decode("nginx\tエンジンエックス\t5\r\n").single().accent)
  }

  @Test
  fun `a line whose accent is not plain digits or is out of range is dropped`() {
    for (accent in listOf("", "x", "+3", "-1", " 3", "3 ", "１", "99999999999", "9")) {
      assertEquals(
          emptyList(),
          DictionaryFormat.decode("nginx\tエンジンエックス\t$accent\n"),
          accent,
      )
    }
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
