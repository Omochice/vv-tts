package io.github.omochice.vvtts.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChunkLengthLimitTest {
  @Test
  fun `a chunk no longer than the maximum is returned whole`() {
    assertEquals(listOf("吾輩は猫である"), limitLength("吾輩は猫である", 7))
  }

  @Test
  fun `a longer chunk is cut after the last comma within the maximum`() {
    assertEquals(
        listOf("吾輩は、猫である、", "名前は"),
        limitLength("吾輩は、猫である、名前は", 10),
    )
  }

  @Test
  fun `a comma exactly at the maximum stays with the earlier piece`() {
    assertEquals(listOf("吾輩は、", "猫である"), limitLength("吾輩は、猫である", 4))
  }

  @Test
  fun `full-width and ASCII commas are soft breaks too`() {
    assertEquals(listOf("吾輩，", "猫,", "名前"), limitLength("吾輩，猫,名前", 3))
  }

  @Test
  fun `the rest after a cut is limited again`() {
    assertEquals(
        listOf("吾輩は、", "猫である、", "名前は、", "まだ無い"),
        limitLength("吾輩は、猫である、名前は、まだ無い", 6),
    )
  }

  @Test
  fun `a window without a soft break is cut at the maximum`() {
    assertEquals(listOf("吾輩は猫", "である名", "前"), limitLength("吾輩は猫である名前", 4))
  }

  @Test
  fun `a surrogate pair at the cut moves whole to the later piece`() {
    assertEquals(listOf("吾輩は", "😺であ", "る"), limitLength("吾輩は😺である", 4))
  }

  @Test
  fun `whitespace around a cut is trimmed and produces no empty piece`() {
    assertEquals(listOf("吾輩は、", "猫である"), limitLength("吾輩は、  猫である", 6))
  }

  @Test
  fun `no piece is longer than the maximum`() {
    val chunk = "吾輩は猫である、名前はまだ無い、".repeat(20)

    assertTrue(limitLength(chunk, 49).all { it.length <= 49 })
  }

  @Test
  fun `the pieces put back together reproduce the chunk`() {
    val chunks =
        listOf(
            "吾輩は猫である、名前はまだ無い、".repeat(10),
            "吾輩は猫である名前はまだ無い".repeat(10),
            "吾輩は😺である😺".repeat(10),
        )

    for (chunk in chunks) {
      for (maxLength in 2..17) {
        assertEquals(chunk, limitLength(chunk, maxLength).joinToString(""), "max $maxLength")
      }
    }
  }

  @Test
  fun `a maximum below two is rejected`() {
    assertFailsWith<IllegalArgumentException> { limitLength("吾輩は猫である", 1) }
  }
}
