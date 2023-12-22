package endorh.unican.gcrv.util

import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec2f

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

   val center get() = Vec2f((left + right) / 2F, (top + bottom) / 2F)

   fun contains(p: Vec2f) = p.x in left..<right && p.y in top..<bottom

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

   fun relativize(p: Vec2f) = Vec2f((p.x - left) / width, (p.y - top) / height)
   fun globalize(p: Vec2f) = Vec2f(p.x * width + left, p.y * height + top)
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

   fun contains(p: Vec2d) = p.x in left..<right && p.y in top..<bottom

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

   fun relativize(p: Vec2d) = Vec2d((p.x - left) / width, (p.y - top) / height)
   fun globalize(p: Vec2d) = Vec2d(p.x * width + left, p.y * height + top)
}