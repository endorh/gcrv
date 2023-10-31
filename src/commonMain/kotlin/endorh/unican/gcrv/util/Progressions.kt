package endorh.unican.gcrv.util

/**
 * Like [IntRange], [rangeTo] or [until], but allowing decreasing ranges.
 * ```
 * for (i in 3 towards 0) println(i) // Prints 3, 2, 1, 0
 * ```
 */
infix fun Int.towards(end: Int): IntProgression =
   IntProgression.fromClosedRange(this, end, if (this <= end) 1 else -1)