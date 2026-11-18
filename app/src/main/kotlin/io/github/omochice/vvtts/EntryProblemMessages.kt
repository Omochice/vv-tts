package io.github.omochice.vvtts

import io.github.omochice.vvtts.dictionary.EntryProblem

/** Returns the string resource that tells the user what to fix. */
internal fun EntryProblem.messageRes(): Int =
    when (this) {
      EntryProblem.EMPTY_WORD -> R.string.dictionary_error_empty_word
      EntryProblem.UNUSABLE_CHARACTER_IN_WORD -> R.string.dictionary_error_unusable_character
      EntryProblem.SENTENCE_END_IN_WORD -> R.string.dictionary_error_sentence_end
      EntryProblem.EMPTY_READING -> R.string.dictionary_error_empty_reading
      EntryProblem.NOT_KANA_READING -> R.string.dictionary_error_not_kana
      EntryProblem.MISPLACED_SMALL_KANA -> R.string.dictionary_error_small_kana
    }
