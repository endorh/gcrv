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
val Double.squared get() = this * this

infix fun Int.pow(exp: Int) = when (this) {
   0 -> 0
   1 -> 1
   2 -> 0x1 shl exp
   else -> {
      var r = 1
      var b = this
      var e = exp
      while (e != 0) {
         if (e % 2 == 1) r *= b
         e /= 2
         b *= b
      }
      r
   }
}

infix fun UInt.pow(exp: UInt) = when (this) {
   0U -> 0U
   1U -> 1U
   2U -> 0x1U shl exp.I
   else -> {
      var r = 1U
      var b = this
      var e = exp
      while (e != 0U) {
         if (e % 2U == 1U) r *= b
         e /= 2U
         b *= b
      }
      r
   }
}

infix fun Long.pow(exp: Int) = when (this) {
   0L -> 0L
   1L -> 1L
   2L -> 0x1L shl exp
   else -> {
      var r = 1L
      var b = this
      var e = exp
      while (e != 0) {
         if (e % 2 == 1) r *= b
         e /= 2
         b *= b
      }
      r
   }
}

infix fun ULong.pow(exp: UInt) = when (this) {
   0UL -> 0UL
   1UL -> 1UL
   2UL -> 0x1UL shl exp.I
   else -> {
      var r = 1UL
      var b = this
      var e = exp
      while (e != 0U) {
         if (e % 2U == 1U) r *= b
         e /= 2U
         b *= b
      }
      r
   }
}