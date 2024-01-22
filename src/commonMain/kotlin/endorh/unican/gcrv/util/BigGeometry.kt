package endorh.unican.gcrv.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import endorh.unican.gcrv.serialization.Vec2d
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.serialization.Vec2i

data class Vec2BD(
   val x: BigDecimal,
   val y: BigDecimal,
) {
   constructor(x: Int, y: Int) : this(x.BD, y.BD)
   constructor(x: Float, y: Float) : this(x.BD, y.BD)
   constructor(x: Double, y: Double) : this(x.BD, y.BD)

   operator fun plus(v: Vec2BD) = Vec2BD(x + v.x, y + v.y)
   operator fun minus(v: Vec2BD) = Vec2BD(x - v.x, y - v.y)
   operator fun times(s: BigDecimal) = Vec2BD(x * s, y * s)
   operator fun div(s: BigDecimal) = Vec2BD(x / s, y / s)

   operator fun plus(v: Vec2d) = Vec2BD(x + v.x.BD, y + v.y.BD)
   operator fun minus(v: Vec2d) = Vec2BD(x - v.x.BD, y - v.y.BD)
   operator fun times(s: Double) = Vec2BD(x * s.BD, y * s.BD)
   operator fun div(s: Double) = Vec2BD(x / s.BD, y / s.BD)

   operator fun plus(v: Vec2f) = Vec2BD(x + v.x.BD, y + v.y.BD)
   operator fun minus(v: Vec2f) = Vec2BD(x - v.x.BD, y - v.y.BD)
   operator fun times(s: Float) = Vec2BD(x * s.BD, y * s.BD)
   operator fun div(s: Float) = Vec2BD(x / s.BD, y / s.BD)

   operator fun plus(v: Vec2i) = Vec2BD(x + v.x.BD, y + v.y.BD)
   operator fun minus(v: Vec2i) = Vec2BD(x - v.x.BD, y - v.y.BD)
   operator fun times(s: Int) = Vec2BD(x * s.BD, y * s.BD)
   operator fun div(s: Int) = Vec2BD(x / s.BD, y / s.BD)

   operator fun unaryMinus() = Vec2BD(-x, -y)
}

data class Rect2BD(
   val left: BigDecimal,
   val top: BigDecimal,
   val right: BigDecimal,
   val bottom: BigDecimal,
) {
   val topLeft get() = Vec2BD(left, top)
   val topRight get() = Vec2BD(right, top)
   val bottomLeft get() = Vec2BD(left, bottom)
   val bottomRight get() = Vec2BD(right, bottom)

   val width get() = right - left
   val height get() = bottom - top

   val center get() = Vec2BD((left + right) / BigDecimal.TWO, (top + bottom) / BigDecimal.TWO)

   fun contains(p: Vec2BD) = p.x in left..<right && p.y in top..<bottom

   fun translate(v: Vec2BD) = Rect2BD(left + v.x, top + v.y, right + v.x, bottom + v.y)
   fun scale(s: BigDecimal) = Rect2BD(left * s, top * s, right * s, bottom * s)
   fun scale(s: Vec2BD) = Rect2BD(left * s.x, top * s.y, right * s.x, bottom * s.y)
   fun scaleFrom(center: Vec2BD, s: BigDecimal) = Rect2BD(
      center.x + (left - center.x) * s,
      center.y + (top - center.y) * s,
      center.x + (right - center.x) * s,
      center.y + (bottom - center.y) * s
   )

   fun unionRect(r: Rect2BD) = Rect2BD(
      minOf(left, r.left),
      minOf(top, r.top),
      maxOf(right, r.right),
      maxOf(bottom, r.bottom)
   )

   fun intersectionRect(r: Rect2BD) = Rect2BD(
      maxOf(left, r.left),
      maxOf(top, r.top),
      minOf(right, r.right),
      minOf(bottom, r.bottom)
   )

   fun relativize(p: Vec2BD) = Vec2BD((p.x - left) / width, (p.y - top) / height)
   fun globalize(p: Vec2BD) = Vec2BD(p.x * width + left, p.y * height + top)
}