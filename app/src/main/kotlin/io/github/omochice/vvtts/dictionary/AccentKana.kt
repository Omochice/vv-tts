package io.github.omochice.vvtts.dictionary

private const val LONG_VOWEL = 'ー'
private const val ACCENT_MARK = '\''

private val VOWEL_OF: Map<Char, Char> =
    mapOf(
            'ア' to "ァアカガサザタダナハバパマャヤラワヮ",
            'イ' to "ィイキギシジチヂニヒビピミリヰ",
            'ウ' to "ゥウクグスズツヅヌフブプムュユルヴ",
            'エ' to "ェエケゲセゼテデネヘベペメレヱ",
            'オ' to "ォオコゴソゾトドノホボポモョヨロヲ",
        )
        .flatMap { (vowel, kana) -> kana.map { it to vowel } }
        .toMap()

/**
 * Returns [reading], a reading in katakana, with each ー written as the vowel of the kana before it,
 * as VOICEVOX's kana notation spells a long vowel: キュー becomes キュウ, ローー becomes ロオオ.
 *
 * Returns null when a ー follows nothing, ン or ッ, which have no vowel to lengthen.
 */
fun kanaWithoutLongVowels(reading: String): String? {
  val kana = StringBuilder(reading.length)
  for (char in reading) {
    if (char == LONG_VOWEL) {
      // Picking some vowel would preview a sound the user did not type, while giving up only leaves
      // the word flat.
      kana.append(kana.lastOrNull()?.let(VOWEL_OF::get) ?: return null)
    } else {
      kana.append(char)
    }
  }
  return kana.toString()
}

/**
 * Returns [kana] in VOICEVOX's kana notation for a pitch that falls after the [accent]-th mora,
 * given the number of characters of each mora in [moraLengths].
 *
 * Flat, an [accent] of 0, carries the mark at the end like a fall after the last mora, because the
 * notation needs a mark in every accent phrase and nothing follows the word.
 *
 * Returns null when [accent] is outside 0 to the number of moras, or when [moraLengths] do not
 * cover [kana] exactly.
 */
fun markAccent(kana: String, moraLengths: List<Int>, accent: Int): String? {
  if (!covers(moraLengths, kana) || accent !in 0..moraLengths.size) return null
  val position = if (accent == 0) kana.length else moraLengths.take(accent).sum()
  return kana.substring(0, position) + ACCENT_MARK + kana.substring(position)
}

/**
 * Returns [reading] cut into consecutive pieces of [lengths] characters, or null when [lengths] do
 * not cover [reading] exactly.
 */
fun moraPieces(reading: String, lengths: List<Int>): List<String>? {
  if (!covers(lengths, reading)) return null
  val starts = lengths.runningFold(0, Int::plus)
  return lengths.indices.map { reading.substring(starts[it], starts[it + 1]) }
}

private fun covers(lengths: List<Int>, text: String): Boolean =
    lengths.all { it > 0 } && lengths.sum() == text.length
