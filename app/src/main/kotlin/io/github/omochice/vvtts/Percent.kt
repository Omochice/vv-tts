package io.github.omochice.vvtts

/**
 * Returns [doneBytes] as a whole percentage of [totalBytes], from 0 to 100; 0 while it is unknown.
 */
fun wholePercent(doneBytes: Long, totalBytes: Long): Int =
    if (totalBytes <= 0) 0 else (doneBytes.coerceIn(0, totalBytes) * 100 / totalBytes).toInt()
