package endorh.unican.gcrv.util

import de.fabmax.kool.math.Vec2f
import kotlin.math.acos
import kotlin.math.sign

operator fun Vec2f.unaryMinus() = Vec2f(-x, -y)
operator fun Vec2f.unaryPlus() = this
operator fun Vec2f.plus(v: Vec2f) = Vec2f(x + v.x, y + v.y)
operator fun Vec2f.minus(v: Vec2f) = Vec2f(x - v.x, y - v.y)
operator fun Vec2f.times(s: Float) = Vec2f(x * s, y * s)
operator fun Vec2f.div(s: Float) = Vec2f(x / s, y / s)

fun Vec2f.angle(other: Vec2f) = acos((this * other) / (length() * other.length()))
private fun Vec2f.cross2D(other: Vec2f) = x * other.y - y * other.x
fun Vec2f.signedAngle(other: Vec2f) = angle(other) * sign(cross2D(other))