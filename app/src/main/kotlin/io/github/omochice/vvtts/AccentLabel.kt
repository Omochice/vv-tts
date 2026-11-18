package io.github.omochice.vvtts

private const val FALL = '＼'

/**
 * Returns [moras] written as one reading with ＼ after the [accent]-th mora, where the pitch falls.
 */
internal fun withFall(moras: List<String>, accent: Int): String =
    moras.take(accent).joinToString("") + FALL + moras.drop(accent).joinToString("")
