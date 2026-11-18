package io.github.omochice.vvtts.dictionary

import io.github.omochice.vvtts.text.isSentenceEnd

/**
 * A word the user wants read in a given way.
 *
 * Obtained only through [of], so every instance passed the checks VOICEVOX would otherwise fail on.
 *
 * @property word the text to match, trimmed.
 * @property reading the pronunciation in katakana.
 */
@ConsistentCopyVisibility
data class DictionaryEntry internal constructor(val word: String, val reading: String) {
  /** Validates user input. */
  companion object {
    /** Checks [word] and [reading], accepting a reading in hiragana or katakana. */
    fun of(word: String, reading: String): EntryCheck {
      val trimmedWord = word.trim()
      val katakana = toKatakana(reading.trim())
      val problem = wordProblem(trimmedWord) ?: readingProblem(katakana)
      return if (problem == null) {
        EntryCheck.Valid(DictionaryEntry(trimmedWord, katakana))
      } else {
        EntryCheck.Invalid(problem)
      }
    }
  }
}

// VOICEVOX CORE crashes the whole process on a surface that is empty or holds a newline, so every
// character that can break a line, a line-based file or a JNI string is refused rather than only
// the measured ones.
private fun wordProblem(word: String): EntryProblem? =
    when {
      word.isEmpty() -> EntryProblem.EMPTY_WORD
      word.indices.any { isUnusable(word, it) } -> EntryProblem.UNUSABLE_CHARACTER_IN_WORD
      word.any(::isSentenceEnd) -> EntryProblem.SENTENCE_END_IN_WORD
      else -> null
    }

private fun isUnusable(text: String, index: Int): Boolean {
  val char = text[index]
  return when {
    char.isISOControl() || char == '\u2028' || char == '\u2029' -> true
    char.isHighSurrogate() -> text.getOrNull(index + 1)?.isLowSurrogate() != true
    char.isLowSurrogate() -> text.getOrNull(index - 1)?.isHighSurrogate() != true
    else -> false
  }
}

private fun readingProblem(reading: String): EntryProblem? =
    when {
      reading.isEmpty() -> EntryProblem.EMPTY_READING
      !ACCEPTED_READING.matches(reading) -> EntryProblem.NOT_KANA_READING
      hasMisplacedSmallKana(reading) -> EntryProblem.MISPLACED_SMALL_KANA
      else -> null
    }

// The pattern VOICEVOX CORE 0.17.1 checks pronunciations against, as found in its native library.
private val ACCEPTED_READING = Regex("^[ァ-ヴー]+$")

private const val SMALL_KANA = "ァィゥェォャュョヮ"
private const val SOKUON = 'ッ'
private const val SMALL_WA = 'ヮ'

// Ported from the small kana rules of the VOICEVOX engine, which VOICEVOX CORE took over.
// They could not be confirmed against the native library, so a small wa at the start is refused as
// well.
// Refusing a reading VOICEVOX would take costs the user one more try, while accepting one it
// rejects leaves an entry that is never applied.
private fun hasMisplacedSmallKana(reading: String): Boolean =
    reading.indices.any { i ->
      val char = reading[i]
      val next = reading.getOrNull(i + 1)
      val previous = reading.getOrNull(i - 1)
      ((char in SMALL_KANA || char == SOKUON) && next != null && next in SMALL_KANA) ||
          (char == SOKUON && next == SOKUON) ||
          (char == SMALL_WA && (previous != 'ク' && previous != 'グ'))
    }

private const val HIRAGANA_TO_KATAKANA = 'ア' - 'あ'

private fun toKatakana(text: String): String =
    String(
        CharArray(text.length) {
          text[it].let { c -> if (c in 'ぁ'..'ゖ') c + HIRAGANA_TO_KATAKANA else c }
        }
    )

/** The outcome of [DictionaryEntry.of]. */
sealed interface EntryCheck {
  /** The input is usable as [entry]. */
  data class Valid(val entry: DictionaryEntry) : EntryCheck

  /** The input breaks the rule [problem]. */
  data class Invalid(val problem: EntryProblem) : EntryCheck
}

/** The rule an input to [DictionaryEntry.of] broke. */
enum class EntryProblem {
  /** The word is empty after trimming. */
  EMPTY_WORD,

  /** The word holds a control character, a line or paragraph separator or an unpaired surrogate. */
  UNUSABLE_CHARACTER_IN_WORD,

  /** The word holds a character the sentence splitter cuts at. */
  SENTENCE_END_IN_WORD,

  /** The reading is empty after trimming. */
  EMPTY_READING,

  /** The reading, in katakana, holds something other than ァ to ヴ and the long vowel mark. */
  NOT_KANA_READING,

  /** The reading has small kana in a sequence VOICEVOX rejects. */
  MISPLACED_SMALL_KANA,
}
