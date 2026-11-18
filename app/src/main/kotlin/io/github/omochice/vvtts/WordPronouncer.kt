package io.github.omochice.vvtts

/** Offers where the pitch of a word can fall, and speaks the word with one of them. */
interface WordPronouncer {
  /**
   * Returns the moras of [reading], a valid dictionary reading in katakana, with the accent to
   * suggest; null when no accent can be offered for it, so it can only be stored flat.
   *
   * Loads the voice model of [voice] if needed, because VOICEVOX's kana parser runs a style.
   */
  fun accentChoices(reading: String, voice: SpeakableVoice): AccentChoices?

  /**
   * Returns [reading] alone, spoken by [voice] with its pitch falling after the [accent]-th mora,
   * or flat for 0, in the PCM format of [SpeechEngine.synthesize].
   *
   * @throws IllegalArgumentException if [accentChoices] offers nothing for [reading] or [accent] is
   *   beyond its moras.
   */
  fun speak(reading: String, accent: Int, voice: SpeakableVoice): ByteArray
}

/**
 * The positions the pitch of a reading can fall at.
 *
 * @property moras the reading as typed, cut into its moras; an accent of n falls after the n-th.
 * @property suggested the accent to preselect, 0 for flat.
 */
data class AccentChoices(val moras: List<String>, val suggested: Int)
