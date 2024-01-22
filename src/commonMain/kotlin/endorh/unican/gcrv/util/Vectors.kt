package endorh.unican.gcrv.util

import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.toRad
import endorh.unican.gcrv.serialization.Vec2i
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

operator fun Vec2i.unaryMinus() = Vec2i(-x, -y)
operator fun Vec2i.unaryPlus() = this
operator fun Vec2i.plus(v: Vec2i) = Vec2i(x + v.x, y + v.y)
operator fun Vec2i.minus(v: Vec2i) = Vec2i(x - v.x, y - v.y)
operator fun Vec2i.times(s: Int) = Vec2i(x * s, y * s)
operator fun Vec2i.div(s: Int) = Vec2i(x / s, y / s)
operator fun Vec2i.rem(s: Int) = Vec2i(x % s, y % s)
operator fun Int.times(s: Vec2i) = s * this
operator fun Int.div(s: Vec2i) = Vec2i(this / s.x, this / s.y)
operator fun Int.rem(s: Vec2i) = Vec2i(this % s.x, this % s.y)

infix fun Vec2i.cross(v: Vec2i) = x * v.y - y * v.x
infix fun Vec2i.dot(v: Vec2i) = x * v.x + y * v.y

operator fun Vec2f.unaryMinus() = Vec2f(-x, -y)
operator fun Vec2f.unaryPlus() = this
operator fun Vec2f.plus(v: Vec2f) = Vec2f(x + v.x, y + v.y)
operator fun Vec2f.minus(v: Vec2f) = Vec2f(x - v.x, y - v.y)
operator fun Vec2f.times(s: Float) = Vec2f(x * s, y * s)
operator fun Vec2f.div(s: Float) = Vec2f(x / s, y / s)
operator fun Float.times(s: Vec2f) = s * this
operator fun Float.div(s: Vec2f) = Vec2f(this / s.x, this / s.y)

fun Vec2f.rotateRad(angleRad: Float, result: MutableVec2f = MutableVec2f()) =
   result.set(this).rotateRad(angleRad)
fun MutableVec2f.rotateRad(angleRad: Float): MutableVec2f = apply {
   val cos = cos(angleRad)
   val sin = sin(angleRad)
   val rx = x * cos - y * sin
   val ry = x * sin + y * cos
   x = rx
   y = ry
   return this
}
fun Vec2f.angle(other: Vec2f) = acos(dot(other) / (length() * other.length()))
fun Vec2f.cross(other: Vec2f) = x * other.y - y * other.x
fun Vec2f.angleSign(other: Vec2f) = sign(cross(other))
fun Vec2f.signedAngle(other: Vec2f) = angle(other) * (if (cross(other) < 0F) -1F else 1F)

fun List<Vec2i>.boundingBox(): Rect2i {
   var left = Int.MAX_VALUE
   var top = Int.MAX_VALUE
   var right = Int.MIN_VALUE
   var bottom = Int.MIN_VALUE
   for (p in this) {
      if (p.x < left) left = p.x
      if (p.x > right) right = p.x
      if (p.y < top) top = p.y
      if (p.y > bottom) bottom = p.y
   }
   return Rect2i(left, top, right, bottom)
}

fun List<Vec2f>.boundingBox(): Rect2f {
   var left = Float.POSITIVE_INFINITY
   var top = Float.POSITIVE_INFINITY
   var right = Float.NEGATIVE_INFINITY
   var bottom = Float.NEGATIVE_INFINITY
   for (p in this) {
      if (p.x < left) left = p.x
      if (p.x > right) right = p.x
      if (p.y < top) top = p.y
      if (p.y > bottom) bottom = p.y
   }
   return Rect2f(left, top, right, bottom)
}