package endorh.unican.gcrv.util

import de.fabmax.kool.util.Color

/**
 * Merges a color as if painted over another.
 *
 * Color channels are interpolated according to the alpha value of the paint, or the
 * inverse alpha channel of the underlying color, whichever is greater.
 *
 * The resulting alpha value is the maximum of the two alpha values.
 */
infix fun Color.paintOver(other: Color): Color {
   if (a == 1F || other.a == 0F) return this // Shortcut (same result as below)
   val aa = maxOf(a, 1F - other.a)
   return Color(
      r = r * aa + other.r * (1F - aa),
      g = g * aa + other.g * (1F - aa),
      b = b * aa + other.b * (1F - aa),
      a = maxOf(a, other.a)
   )
}