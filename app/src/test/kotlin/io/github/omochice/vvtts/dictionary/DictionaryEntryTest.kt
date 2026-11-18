package io.github.omochice.vvtts.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals

class DictionaryEntryTest {
  private fun problemOf(word: String, reading: String): EntryProblem? =
      (DictionaryEntry.of(word, reading) as? EntryCheck.Invalid)?.problem

  private fun entryOf(word: String, reading: String): DictionaryEntry =
      (DictionaryEntry.of(word, reading) as EntryCheck.Valid).entry

  private fun accentProblemOf(word: String, reading: String, accent: Int): EntryProblem? =
      (DictionaryEntry.of(word, reading, accent) as? EntryCheck.Invalid)?.problem

  private fun accentedEntryOf(word: String, reading: String, accent: Int): DictionaryEntry =
      (DictionaryEntry.of(word, reading, accent) as EntryCheck.Valid).entry

  @Test
  fun `a word with a katakana reading is kept as given`() {
    val entry = entryOf("nginx", "エンジンエックス")

    assertEquals("nginx", entry.word)
    assertEquals("エンジンエックス", entry.reading)
  }

  @Test
  fun `a hiragana reading is stored in katakana`() {
    assertEquals("エンジンエックス", entryOf("nginx", "えんじんえっくす").reading)
  }

  @Test
  fun `small kana, the voiced u and the long vowel mark convert from hiragana`() {
    assertEquals("ヴァッションー", entryOf("x", "ゔぁっしょんー").reading)
  }

  @Test
  fun `surrounding whitespace is trimmed from both the word and the reading`() {
    val entry = entryOf("  node.js\t", "　ノードジェイエス ")

    assertEquals("node.js", entry.word)
    assertEquals("ノードジェイエス", entry.reading)
  }

  @Test
  fun `spaces, dots, quotes and commas inside a word are allowed`() {
    for (word in listOf("hello world", "node.js", "\"quoted\"", "a,b", "C++")) {
      assertEquals(word, entryOf(word, "テスト").word)
    }
  }

  @Test
  fun `an empty or blank word is rejected`() {
    assertEquals(EntryProblem.EMPTY_WORD, problemOf("", "テスト"))
    assertEquals(EntryProblem.EMPTY_WORD, problemOf(" 　\t", "テスト"))
  }

  @Test
  fun `a word with a control character inside is rejected`() {
    for (word in listOf("a\nb", "a\rb", "a\tb", "a\u0000b", "a\u007Fb", "a\u0085b")) {
      assertEquals(EntryProblem.UNUSABLE_CHARACTER_IN_WORD, problemOf(word, "テスト"), word)
    }
  }

  @Test
  fun `a word with a line or paragraph separator is rejected`() {
    assertEquals(EntryProblem.UNUSABLE_CHARACTER_IN_WORD, problemOf("a\u2028b", "テスト"))
    assertEquals(EntryProblem.UNUSABLE_CHARACTER_IN_WORD, problemOf("a\u2029b", "テスト"))
  }

  @Test
  fun `a word with an unpaired surrogate is rejected but a paired one is allowed`() {
    assertEquals(EntryProblem.UNUSABLE_CHARACTER_IN_WORD, problemOf("a\uD83Db", "テスト"))
    assertEquals(EntryProblem.UNUSABLE_CHARACTER_IN_WORD, problemOf("a\uDE00b", "テスト"))
    assertEquals("a\uD83D\uDE00b", entryOf("a\uD83D\uDE00b", "テスト").word)
  }

  @Test
  fun `a word the splitter would cut is rejected`() {
    for (word in listOf("Yahoo!", "Yahoo！", "why?", "なぜ？", "終わり。")) {
      assertEquals(EntryProblem.SENTENCE_END_IN_WORD, problemOf(word, "テスト"), word)
    }
  }

  @Test
  fun `an empty or blank reading is rejected`() {
    assertEquals(EntryProblem.EMPTY_READING, problemOf("nginx", ""))
    assertEquals(EntryProblem.EMPTY_READING, problemOf("nginx", " 　"))
  }

  @Test
  fun `a reading with anything but kana and the long vowel mark is rejected`() {
    for (reading in listOf("nginx", "エンジン エックス", "漢字", "エンジン-", "ｴﾝｼﾞﾝ", "ナカ・グロ", "ヽ")) {
      assertEquals(EntryProblem.NOT_KANA_READING, problemOf("nginx", reading), reading)
    }
  }

  @Test
  fun `katakana outside the range VOICEVOX accepts is rejected, also when written in hiragana`() {
    for (reading in listOf("ヵ", "ヶ", "ゕ", "ゖ", "ヷ", "ヺ")) {
      assertEquals(EntryProblem.NOT_KANA_READING, problemOf("x", reading), reading)
    }
  }

  @Test
  fun `the whole range VOICEVOX accepts is allowed`() {
    assertEquals("ァヴー", entryOf("x", "ァヴー").reading)
  }

  @Test
  fun `a reading starting with the long vowel mark is allowed`() {
    assertEquals("ーア", entryOf("x", "ーア").reading)
  }

  @Test
  fun `a small kana followed by another small kana other than the sokuon is rejected`() {
    for (reading in listOf("キャァ", "ッャ", "フォォ")) {
      assertEquals(EntryProblem.MISPLACED_SMALL_KANA, problemOf("x", reading), reading)
    }
  }

  @Test
  fun `a doubled sokuon is rejected but a sokuon after another small kana is allowed`() {
    assertEquals(EntryProblem.MISPLACED_SMALL_KANA, problemOf("x", "アッッ"))
    assertEquals("キャット", entryOf("x", "キャット").reading)
  }

  @Test
  fun `the small wa only follows ku or gu`() {
    assertEquals("クヮシ", entryOf("x", "クヮシ").reading)
    assertEquals("グヮン", entryOf("x", "グヮン").reading)
    assertEquals(EntryProblem.MISPLACED_SMALL_KANA, problemOf("x", "アヮ"))
    assertEquals(EntryProblem.MISPLACED_SMALL_KANA, problemOf("x", "ヮア"))
  }

  @Test
  fun `an entry given no accent is flat`() {
    assertEquals(0, entryOf("nginx", "エンジンエックス").accent)
  }

  @Test
  fun `an accent up to the length of the reading is kept`() {
    assertEquals(5, accentedEntryOf("nginx", "エンジンエックス", 5).accent)
    assertEquals(8, accentedEntryOf("nginx", "エンジンエックス", 8).accent)
  }

  @Test
  fun `a negative accent or one beyond the length of the reading is rejected`() {
    assertEquals(EntryProblem.ACCENT_OUT_OF_RANGE, accentProblemOf("nginx", "エンジンエックス", -1))
    assertEquals(EntryProblem.ACCENT_OUT_OF_RANGE, accentProblemOf("nginx", "エンジンエックス", 9))
  }

  @Test
  fun `the accent is bounded by the reading in katakana, after trimming`() {
    assertEquals(3, accentedEntryOf("x", " てすと ", 3).accent)
    assertEquals(EntryProblem.ACCENT_OUT_OF_RANGE, accentProblemOf("x", " てすと ", 4))
  }

  @Test
  fun `the reading is checked before the accent`() {
    assertEquals(EntryProblem.EMPTY_READING, accentProblemOf("x", "", 1))
  }

  @Test
  fun `a reading alone is checked as an entry checks it and returned in katakana`() {
    assertEquals(ReadingCheck.Valid("エンジンエックス"), DictionaryEntry.readingOf(" えんじんえっくす "))
    assertEquals(ReadingCheck.Invalid(EntryProblem.EMPTY_READING), DictionaryEntry.readingOf(" "))
    assertEquals(
        ReadingCheck.Invalid(EntryProblem.MISPLACED_SMALL_KANA),
        DictionaryEntry.readingOf("アッッ"),
    )
  }

  @Test
  fun `the word is checked before the reading`() {
    assertEquals(EntryProblem.EMPTY_WORD, problemOf("", ""))
  }
}
