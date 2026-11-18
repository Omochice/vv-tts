package io.github.omochice.vvtts.voicevox

/** The shape of one accent phrase OpenJTalk's analysis found. */
internal data class AnalyzedPhrase(val moraCount: Int, val accent: Int)

/**
 * Returns the accent to suggest for a reading of [moraCount] moras that the analysis split into
 * [analyzed]: its accent when it is one phrase of the same moras falling inside the word, otherwise
 * flat.
 *
 * The analysis reads the katakana as text, and on an emulator it split トウキョウトッキョキョカキョク into 12
 * moras where the dictionary counts 11, so an analysis of other moras is not trusted.
 */
internal fun suggestedAccent(analyzed: List<AnalyzedPhrase>, moraCount: Int): Int {
  val phrase = analyzed.singleOrNull() ?: return 0
  // A fall after the last mora is how VOICEVOX's phrases appear to write flat, as its kana notation
  // does, and a word alone sounds the same either way, so the far more common flat is suggested.
  return if (phrase.moraCount == moraCount && phrase.accent in 1 until moraCount) {
    phrase.accent
  } else {
    0
  }
}
