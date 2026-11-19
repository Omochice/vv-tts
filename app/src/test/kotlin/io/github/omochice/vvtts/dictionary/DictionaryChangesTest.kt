package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class DictionaryChangesTest {
  @get:Rule val tmp = TemporaryFolder()

  private val file by lazy { UserDictionaryFile(tmp.root) }

  @Test
  fun `the first look returns the stored entries`() {
    file.write("nginx\tエンジンエックス\n")

    assertEquals(listOf(validEntry("nginx", "エンジンエックス")), DictionaryChanges(file).changedEntries())
  }

  @Test
  fun `the first look without a file returns no entries rather than nothing changed`() {
    assertEquals(emptyList(), DictionaryChanges(file).changedEntries())
  }

  @Test
  fun `a second look without a change returns null`() {
    file.write("nginx\tエンジンエックス\n")
    val changes = DictionaryChanges(file)
    changes.changedEntries()

    assertNull(changes.changedEntries())
  }

  @Test
  fun `a look after a write returns the new entries`() {
    val changes = DictionaryChanges(file)
    changes.changedEntries()

    UserDictionary(file).add(validEntry("nginx", "エンジンエックス"))

    assertEquals(listOf(validEntry("nginx", "エンジンエックス")), changes.changedEntries())
  }

  @Test
  fun `a replaced reading of the same length is noticed even with the same modification time`() {
    file.write("nginx\tエンジンエックス\n")
    val changes = DictionaryChanges(file)
    changes.changedEntries()
    val modified = file.path.lastModified()

    file.write("nginx\tエンジンエクスス\n")
    file.path.setLastModified(modified)

    assertEquals(listOf(validEntry("nginx", "エンジンエクスス")), changes.changedEntries())
  }

  @Test
  fun `deleting the file is a change to no entries`() {
    file.write("nginx\tエンジンエックス\n")
    val changes = DictionaryChanges(file)
    changes.changedEntries()

    file.path.delete()

    assertEquals(emptyList(), changes.changedEntries())
  }

  @Test
  fun `a file that changed only in lines that fail validation is a change to the same entries`() {
    file.write("nginx\tエンジンエックス\n")
    val changes = DictionaryChanges(file)
    changes.changedEntries()

    file.write("nginx\tエンジンエックス\nbad\tbad\n")

    assertEquals(listOf(validEntry("nginx", "エンジンエックス")), changes.changedEntries())
  }
}
