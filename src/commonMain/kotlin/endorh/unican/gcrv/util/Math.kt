package endorh.unican.gcrv.util

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

private val TO_RAD = PI.F / 180F
private val TO_DEG = 180F / PI.F

fun rad(degrees: Float) = degrees * TO_RAD
fun deg(radians: Float) = radians * TO_DEG

fun ctg(angle: Float) = 1F / tan(angle)

fun Float.roundDecimals(decimals: Int): Float = (this * 10F.pow(decimals)).roundToInt() / 10F.pow(decimals)

val Int.squared get() = this * this
val Float.squared get() = this * this