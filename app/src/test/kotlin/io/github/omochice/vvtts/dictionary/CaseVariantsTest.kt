package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseVariantsTest {
  private fun entry(word: String, reading: String) =
      (DictionaryEntry.of(word, reading) as EntryCheck.Valid).entry

  private fun wordsOf(vararg entries: DictionaryEntry) = wordsToRegister(entries.toList())

  @Test
  fun `a lower-case word also registers its upper-case and capitalized forms`() {
    assertEquals(
        listOf(
            entry("nginx", "エンジンエックス"),
            entry("NGINX", "エンジンエックス"),
            entry("Nginx", "エンジンエックス"),
        ),
        wordsOf(entry("nginx", "エンジンエックス")),
    )
  }

  @Test
  fun `a mixed-case word keeps its own form first and adds all three others`() {
    assertEquals(
        listOf("GitHub", "github", "GITHUB", "Github"),
        wordsOf(entry("GitHub", "ギットハブ")).map { it.word },
    )
  }

  @Test
  fun `forms that coincide are registered once`() {
    assertEquals(listOf("A", "a"), wordsOf(entry("A", "エー")).map { it.word })
    assertEquals(listOf("Node", "node", "NODE"), wordsOf(entry("Node", "ノード")).map { it.word })
  }

  @Test
  fun `a word without ASCII letters is registered as it is`() {
    assertEquals(listOf("今日"), wordsOf(entry("今日", "コンニチ")).map { it.word })
    assertEquals(listOf("ｎｇｉｎｘ"), wordsOf(entry("ｎｇｉｎｘ", "エンジンエックス")).map { it.word })
  }

  @Test
  fun `only the first letter is capitalized, even when the word starts with another character`() {
    assertEquals(
        listOf(".net", ".NET"),
        wordsOf(entry(".net", "ドットネット")).map { it.word },
    )
    assertEquals(
        listOf("hello world", "HELLO WORLD", "Hello world"),
        wordsOf(entry("hello world", "ハローワールド")).map { it.word },
    )
  }

  @Test
  fun `letters outside ASCII keep their case`() {
    assertEquals(
        listOf("straße", "STRAßE", "Straße"),
        wordsOf(entry("straße", "シュトラーセ")).map { it.word },
    )
  }

  @Test
  fun `the word as stored wins over a variant generated from another entry`() {
    val registered = wordsOf(entry("nginx", "エンジンエックス"), entry("NGINX", "ンギンクス"))

    assertEquals(
        listOf(
            entry("nginx", "エンジンエックス"),
            entry("NGINX", "ンギンクス"),
            entry("Nginx", "エンジンエックス"),
        ),
        registered,
    )
  }
}
