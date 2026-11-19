package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseVariantsTest {
  private fun wordsOf(vararg entries: DictionaryEntry) = wordsToRegister(entries.toList())

  @Test
  fun `a lower-case word also registers its upper-case and capitalized forms`() {
    assertEquals(
        listOf(
            validEntry("nginx", "エンジンエックス"),
            validEntry("NGINX", "エンジンエックス"),
            validEntry("Nginx", "エンジンエックス"),
        ),
        wordsOf(validEntry("nginx", "エンジンエックス")),
    )
  }

  @Test
  fun `every form keeps the accent of its entry`() {
    val accented = validEntry("nginx", "エンジンエックス", 5)

    assertEquals(listOf(5, 5, 5), wordsOf(accented).map { it.accent })
  }

  @Test
  fun `a mixed-case word keeps its own form first and adds all three others`() {
    assertEquals(
        listOf("GitHub", "github", "GITHUB", "Github"),
        wordsOf(validEntry("GitHub", "ギットハブ")).map { it.word },
    )
  }

  @Test
  fun `forms that coincide are registered once`() {
    assertEquals(listOf("A", "a"), wordsOf(validEntry("A", "エー")).map { it.word })
    assertEquals(listOf("Node", "node", "NODE"), wordsOf(validEntry("Node", "ノード")).map { it.word })
  }

  @Test
  fun `a word without ASCII letters is registered as it is`() {
    assertEquals(listOf("今日"), wordsOf(validEntry("今日", "コンニチ")).map { it.word })
    assertEquals(listOf("ｎｇｉｎｘ"), wordsOf(validEntry("ｎｇｉｎｘ", "エンジンエックス")).map { it.word })
  }

  @Test
  fun `only the first letter is capitalized, even when the word starts with another character`() {
    assertEquals(
        listOf(".net", ".NET"),
        wordsOf(validEntry(".net", "ドットネット")).map { it.word },
    )
    assertEquals(
        listOf("hello world", "HELLO WORLD", "Hello world"),
        wordsOf(validEntry("hello world", "ハローワールド")).map { it.word },
    )
  }

  @Test
  fun `letters outside ASCII keep their case`() {
    assertEquals(
        listOf("straße", "STRAßE", "Straße"),
        wordsOf(validEntry("straße", "シュトラーセ")).map { it.word },
    )
  }

  @Test
  fun `the word as stored wins over a variant generated from another entry`() {
    val registered = wordsOf(validEntry("nginx", "エンジンエックス"), validEntry("NGINX", "ンギンクス"))

    assertEquals(
        listOf(
            validEntry("nginx", "エンジンエックス"),
            validEntry("NGINX", "ンギンクス"),
            validEntry("Nginx", "エンジンエックス"),
        ),
        registered,
    )
  }
}
