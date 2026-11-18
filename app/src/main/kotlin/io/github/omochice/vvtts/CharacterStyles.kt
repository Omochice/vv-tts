package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.Voice

/**
 * The styles one character speaks in within a voice model.
 *
 * @property label the text the settings screen shows, such as `ずんだもん（ノーマル、あまあま）`.
 */
data class CharacterStyles(val character: String, val styles: List<String>) {
  val label: String
    get() = styles.joinToString("、", prefix = "$character（", postfix = "）")
}

/** Groups [voices] by character, keeping characters and styles in the order of [voices]. */
fun characterStylesOf(voices: List<Voice>): List<CharacterStyles> =
    voices.groupBy(Voice::character, Voice::style).map { (character, styles) ->
      CharacterStyles(character, styles)
    }
