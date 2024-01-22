package endorh.unican.gcrv.util

import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.math.clamp
import kotlin.math.ceil

data class Rect2i(val left: Int, val top: Int, val right: Int, val bottom: Int) {
   val width get() = right - left
   val height get() = bottom - top

   val area get() = width * height

   val center get() = Vec2i((left + right) / 2, (top + bottom) / 2)
   val xRange get() = left..right
   val yRange get() = top..bottom
   val topLeft get() = Vec2i(left, top)
   val topRight get() = Vec2i(right, top)
   val bottomLeft get() = Vec2i(left, bottom)
   val bottomRight get() = Vec2i(right, bottom)

   operator fun contains(p: Vec2i) = p.x in xRange && p.y in yRange

   val indices: Sequence<Pair<Int, Int>>
      get() = (left..<right).asSequence().flatMap { x ->
         (top..<bottom).asSequence().map { x to it }
      }
   val pixels: Sequence<Vec2i>
      get() = (left..<right).asSequence().flatMap { x ->
         (top..<bottom).asSequence().map { Vec2i(x, it) }
      }

   companion object {
      val ZERO = Rect2i(0, 0, 0, 0)
      val FULL = Rect2i(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
   }
}


data class Rect2f(
   val left: Float,
   val top: Float,
   val right: Float,
   val bottom: Float,
) {
   val topLeft get() = Vec2f(left, top)
   val topRight get() = Vec2f(right, top)
   val bottomLeft get() = Vec2f(left, bottom)
   val bottomRight get() = Vec2f(right, bottom)

   val width get() = right - left
   val height get() = bottom - top
   val area get() = width * height
   val size get() = Vec2f(width, height)

   val center get() = Vec2f((left + right) / 2F, (top + bottom) / 2F)

   operator fun contains(p: Vec2f) = p.x in left..<right && p.y in top..<bottom

   /**
    * Check if a position is contained up to some margin.
    */
   fun containsWithMargin(p: Vec2f, margin: Float): Boolean {
      val m = margin / 2F
      return p.x in left - m..right + m && p.y in top - m..bottom + m
   }

   fun translate(v: Vec2f) = Rect2f(left + v.x, top + v.y, right + v.x, bottom + v.y)
   fun scale(s: Float) = Rect2f(left * s, top * s, right * s, bottom * s)
   fun scale(s: Vec2f) = Rect2f(left * s.x, top * s.y, right * s.x, bottom * s.y)
   fun scaleFrom(center: Vec2f, s: Float) = Rect2f(
      center.x + (left - center.x) * s,
      center.y + (top - center.y) * s,
      center.x + (right - center.x) * s,
      center.y + (bottom - center.y) * s
   )

   fun unionRect(r: Rect2f) = Rect2f(
      minOf(left, r.left),
      minOf(top, r.top),
      maxOf(right, r.right),
      maxOf(bottom, r.bottom)
   )

   fun intersectionRect(r: Rect2f) = Rect2f(
      maxOf(left, r.left),
      maxOf(top, r.top),
      minOf(right, r.right),
      minOf(bottom, r.bottom)
   )

   fun intersectsCircle(center: Vec2f, radius: Float): Boolean {
      val rr = radius*radius
      val closest = clamp(center)
      return (closest.x - center.x).squared + (closest.y - center.y).squared <= rr
   }

   fun clamp(p: Vec2f) = Vec2f(p.x.clamp(left, right), p.y.clamp(top, bottom))

   fun relativize(p: Vec2f) = Vec2f((p.x - left) / width, (p.y - top) / height)
   fun globalize(p: Vec2f) = Vec2f(p.x * width + left, p.y * height + top)

   fun isEmpty() = width <= 0F || height <= 0F
   fun isNotEmpty() = width > 0F && height > 0F

   fun toRect2i() = Rect2i(left.I, top.I, right.I, bottom.I)
   fun growToRect2i() = Rect2i(left.I, top.I, ceil(right).I, ceil(bottom).I)
   fun toRect2d() = Rect2d(left.D, top.D, right.D, bottom.D)

   companion object {
      val ZERO = Rect2f(0F, 0F, 0F, 0F)
      val UNIT = Rect2f(0F, 0F, 1F, 1F)
      val CENTERED_UNIT = Rect2f(-1F, -1F, 1F, 1F)
      val FULL = Rect2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
   }
}

data class Rect2d(
   val left: Double,
   val top: Double,
   val right: Double,
   val bottom: Double,
) {
   val topLeft get() = Vec2d(left, top)
   val topRight get() = Vec2d(right, top)
   val bottomLeft get() = Vec2d(left, bottom)
   val bottomRight get() = Vec2d(right, bottom)

   val width get() = right - left
   val height get() = bottom - top

   val center get() = Vec2d((left + right) / 2.0, (top + bottom) / 2.0)

   operator fun contains(p: Vec2d) = p.x in left..<right && p.y in top..<bottom

   fun translate(v: Vec2d) = Rect2d(left + v.x, top + v.y, right + v.x, bottom + v.y)
   fun scale(s: Double) = Rect2d(left * s, top * s, right * s, bottom * s)
   fun scale(s: Vec2d) = Rect2d(left * s.x, top * s.y, right * s.x, bottom * s.y)
   fun scaleFrom(center: Vec2d, s: Double) = Rect2d(
      center.x + (left - center.x) * s,
      center.y + (top - center.y) * s,
      center.x + (right - center.x) * s,
      center.y + (bottom - center.y) * s
   )

   fun unionRect(r: Rect2d) = Rect2d(
      minOf(left, r.left),
      minOf(top, r.top),
      maxOf(right, r.right),
      maxOf(bottom, r.bottom)
   )

   fun intersectionRect(r: Rect2d) = Rect2d(
      maxOf(left, r.left),
      maxOf(top, r.top),
      minOf(right, r.right),
      minOf(bottom, r.bottom)
   )

   fun intersectsCircle(center: Vec2d, radius: Float): Boolean {
      val rr = radius * radius
      val closest = clamp(center)
      return (closest.x - center.x).squared + (closest.y - center.y).squared <= rr
   }

   fun clamp(p: Vec2d) = Vec2d(p.x.clamp(left, right), p.y.clamp(top, bottom))

   fun relativize(p: Vec2d) = Vec2d((p.x - left) / width, (p.y - top) / height)
   fun globalize(p: Vec2d) = Vec2d(p.x * width + left, p.y * height + top)

   fun isEmpty() = width <= 0.0 || height <= 0.0
   fun isNotEmpty() = width > 0.0 && height > 0.0

   fun toRect2i() = Rect2i(left.I, top.I, right.I, bottom.I)
   fun toRect2f() = Rect2f(left.F, top.F, right.F, bottom.F)

   companion object {
      val ZERO = Rect2d(0.0, 0.0, 0.0, 0.0)
      val UNIT = Rect2d(0.0, 0.0, 1.0, 1.0)
      val CENTERED_UNIT = Rect2d(-1.0, -1.0, 1.0, 1.0)
      val FULL = Rect2d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
   }
}