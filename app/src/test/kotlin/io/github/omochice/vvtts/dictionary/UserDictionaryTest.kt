package io.github.omochice.vvtts.dictionary

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class UserDictionaryTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun file() = UserDictionaryFile(tmp.root)

  private fun dictionary() = UserDictionary(file())

  private fun entry(word: String, reading: String) =
      (DictionaryEntry.of(word, reading) as EntryCheck.Valid).entry

  @Test
  fun `the file lives directly under the given directory`() {
    assertEquals(File(tmp.root, "user-dictionary.tsv"), file().path)
  }

  @Test
  fun `a missing file reads as empty`() {
    assertEquals("", file().read())
    assertEquals(emptyList(), dictionary().entries())
  }

  @Test
  fun `written text is read back`() {
    file().write("nginx\tエンジンエックス\n")

    assertEquals("nginx\tエンジンエックス\n", file().read())
  }

  @Test
  fun `writing replaces the file and leaves no temporary file behind`() {
    file().write("first\tファースト\n")
    file().write("second\tセカンド\n")

    assertEquals("second\tセカンド\n", file().read())
    assertEquals(listOf("user-dictionary.tsv"), tmp.root.list()?.toList())
  }

  @Test
  fun `writing creates the directory when it is missing`() {
    val nested = UserDictionaryFile(File(tmp.root, "files"))

    nested.write("a\tア\n")

    assertEquals("a\tア\n", nested.read())
  }

  @Test
  fun `an added entry is listed and stored`() {
    dictionary().add(entry("nginx", "エンジンエックス"))

    assertEquals(listOf(entry("nginx", "エンジンエックス")), dictionary().entries())
    assertEquals("nginx\tエンジンエックス\t0\n", file().read())
  }

  @Test
  fun `entries are listed in the order of their words whatever order they were added in`() {
    dictionary().add(entry("node.js", "ノードジェイエス"))
    dictionary().add(entry("Apache", "アパッチ"))
    dictionary().add(entry("nginx", "エンジンエックス"))

    assertEquals(listOf("Apache", "nginx", "node.js"), dictionary().entries().map { it.word })
  }

  @Test
  fun `adding a word that exists replaces its reading`() {
    dictionary().add(entry("nginx", "エンジンエックス"))
    dictionary().add(entry("nginx", "エンジンエクス"))

    assertEquals(listOf(entry("nginx", "エンジンエクス")), dictionary().entries())
  }

  @Test
  fun `adding a word that differs only in ASCII case replaces the existing one`() {
    dictionary().add(entry("nginx", "エンジンエックス"))
    dictionary().add(entry("Nginx", "エンジンエクス"))

    assertEquals(listOf(entry("Nginx", "エンジンエクス")), dictionary().entries())
  }

  @Test
  fun `removing a word drops only that entry`() {
    dictionary().add(entry("nginx", "エンジンエックス"))
    dictionary().add(entry("node.js", "ノードジェイエス"))

    dictionary().remove(entry("nginx", "エンジンエックス"))

    assertEquals(listOf(entry("node.js", "ノードジェイエス")), dictionary().entries())
  }

  @Test
  fun `removing a word that is not stored changes nothing`() {
    dictionary().add(entry("nginx", "エンジンエックス"))

    dictionary().remove(entry("apache", "アパッチ"))

    assertEquals(listOf(entry("nginx", "エンジンエックス")), dictionary().entries())
  }

  @Test
  fun `a file edited by hand with invalid lines lists only the valid ones`() {
    file().write("\tエンジン\nnginx\tエンジンエックス\nbad\tbad\n")

    assertEquals(listOf(entry("nginx", "エンジンエックス")), dictionary().entries())
  }

  @Test
  fun `adding to a file with invalid lines drops them from the file`() {
    file().write("bad\tbad\nnginx\tエンジンエックス\n")

    dictionary().add(entry("node.js", "ノードジェイエス"))

    assertTrue("bad" !in file().read())
  }
}
