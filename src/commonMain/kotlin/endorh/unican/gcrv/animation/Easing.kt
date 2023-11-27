package endorh.unican.gcrv.animation

import kotlin.math.*

val Easings = mutableListOf(
   Easing.Linear,
   Easing.EaseIn,
   Easing.EaseOut,
   Easing.EaseInOut,
   Easing.EaseCircIn,
   Easing.EaseCircOut,
   Easing.EaseCircInOut,
   Easing.EaseCubicIn,
   Easing.EaseCubicOut,
   Easing.EaseCubicInOut,
   Easing.EaseElasticIn(),
   Easing.CubicBezier(0.5F, 0F, 0.5F, 1F),
)

interface Easing {
   fun ease(t: Float): Float

   object Linear : Easing {
      override fun ease(t: Float): Float = t
   }

   object EaseIn : Easing {
      override fun ease(t: Float): Float = t * t
   }

   object EaseOut : Easing {
      override fun ease(t: Float): Float = t * (2 - t)
   }

   object EaseInOut : Easing {
      override fun ease(t: Float): Float = if (t < 0.5F) 2*t*t else (4 - 2*t) * t - 1
   }

   object EaseCircIn : Easing {
      override fun ease(t: Float): Float = 1 - sqrt(1 - t*t)
   }

   object EaseCircOut : Easing {
      override fun ease(t: Float): Float = sqrt(1 - (t - 1)*(t - 1))
   }

   object EaseCircInOut : Easing {
      override fun ease(t: Float): Float = if (t < 0.5F) 0.5F * (1 - sqrt(1 - 4*t*t)) else 0.5F * (sqrt(-((2*t - 3)*(2*t - 1))) + 1)
   }

   object EaseCubicIn : Easing {
      override fun ease(t: Float): Float = t * t * t
   }

   object EaseCubicOut : Easing {
      override fun ease(t: Float): Float = 1 - (1 - t) * (1 - t) * (1 - t)
   }

   object EaseCubicInOut : Easing {
      override fun ease(t: Float): Float = if (t < 0.5F) 4*t*t*t else 1 + 4*(t - 1)*(t - 1)*(t - 1)
   }

   class EaseElasticIn(private val amplitude: Float = 1F, private val period: Float = 0.3F) : Easing {
      override fun ease(t: Float): Float {
         if (t == 0F || t == 1F) return t
         val s = period / 4
         val tr = t - 1
         return -(amplitude * 2F.pow(10 * tr) * sin((tr - s) * (2 * PI) / period)).toFloat()
      }
   }

   data class CubicBezier(
      val xS: Float, val yS: Float, val xE: Float, val yE: Float
   ) : Easing {
      init {
         if (xS < 0 || xS > 1 || xE < 0 || xE > 1) throw IllegalArgumentException(
            "Bezier easing function control point x coordinates must be in [0, 1] range")
      }

      private val sampleValues: FloatArray? = if (xS != yS || xE != yE) {
         calcSampleValues()
      } else null // Short-circuit linear bezier

      /**
       * Precompute an array of samples
       */
      private fun calcSampleValues() = FloatArray(SAMPLE_TABLE_SIZE).apply {
         for (i in 0 until SAMPLE_TABLE_SIZE)
            this[i] = calcBezier(i * SAMPLE_STEP_SIZE, xS, xE)
      }

      /**
       * Degree 3 coefficient of a 1D cubic bezier polynomial.
       * @param p1 control point 1
       * @param p2 control point 2
       * @return `1 - 3p2 + 3p1`
       */
      private fun A(p1: Float, p2: Float) = 1 - 3*p2 + 3*p1

      /**
       * Degree 2 coefficient of a 1D cubic bezier polynomial.
       * @param p1 control point 1
       * @param p2 control point 2
       * @return `3p2 - 6p1`
       */
      private fun B(p1: Float, p2: Float) = 3*p2 - 6*p1

      /**
       * Degree 1 coefficient of a 1D cubic bezier polynomial.
       * @param p1 control point 1
       * @return `3p1`
       */
      private fun C(p1: Float) = 3*p1

      /**
       * Returns `x(t)` given `t`, `xS` and `xE`, or `y(t)` given `t`, `yS` and `yE`
       * @param t Bezier parameter
       * @param p1 `x` or `y` coordinate of control point 1
       * @param p2 `x` or `y` coordinate of control point 2
       * @return `x(t)` or `y(t)`
       */
      private fun calcBezier(t: Float, p1: Float, p2: Float) =
         t * (t * (t * A(p1, p2) + B(p1, p2)) + C(p1))

      /**
       * Returns `dx/dt` given `t`, `xS`, and `xE`, or `dy/dt` given `t`, `yS` and `yE`
       * @param t Bezier parameter
       * @param p1 `x` or `y` coordinate of control point 1
       * @param p2 `x` or `y` coordinate of control point 2
       * @return `dx/dt` or `dy/dt`
       */
      private fun getSlope(t: Float, p1: Float, p2: Float): Float {
         return t * (t * 3 * A(p1, p2) + 2 * B(p1, p2)) + C(p1)
      }

      /**
       * Finds a `t` value for which `calcBezier(t) = x`.
       *
       * Must **not** be called for a linear bezier.
       * @param `x` value to solve for
       * @return Approximate `t` value for which `calcBezier(t) = x`
       */
      private fun getTForX(x: Float): Float {
         // Find sample step where the x is
         var currentIndex = 1
         val lastIndex = SAMPLE_TABLE_SIZE - 1
         while (currentIndex < lastIndex && sampleValues!![currentIndex] <= x) currentIndex++
         val intervalStart = --currentIndex * SAMPLE_STEP_SIZE

         // Linear guess
         val current = sampleValues!![currentIndex]
         val next = sampleValues[currentIndex + 1]
         val dist = (x - current) / (next - current)
         val tGuess = intervalStart + dist * SAMPLE_STEP_SIZE

         // Check the slope to choose a strategy.
         // If the slope is too small, Newton-Raphson won't converge.
         val initialSlope = getSlope(tGuess, xS, xE)

         return if (initialSlope >= NEWTON_MIN_SLOPE) newtonRaphsonIterate(x, tGuess)
         else if (initialSlope == 0F) tGuess
         else binarySubdivide(x, intervalStart, intervalStart + SAMPLE_STEP_SIZE)
      }

      /**
       * Finds a `t` value for which `calcBezier(t) = x` using the Newton-Raphson method.
       * @param `x` value to solve for
       * @param `tGuess` Initial guess for `t`
       * @return Approximate `t` value for which `calcBezier(t) = x`
       */
      private fun newtonRaphsonIterate(x: Float, tGuess: Float): Float {
         // Find a root for `f(t) = calcBezier(t) - x`
         var t = tGuess
         var currentX: Float
         var currentSlope: Float
         for (i in 0 until NEWTON_ITERATIONS) {
            currentX = calcBezier(t, xS, xE) - x
            currentSlope = getSlope(t, xS, xE)
            if (currentSlope == 0F) return t
            t -= currentX / currentSlope
         }
         return t
      }

      /**
       * Finds a `t` value for which `calcBezier(t) = x` using binary search.
       * @param `x` value to solve for
       * @param `l` Initial lower bound for `t`
       * @param `r` Initial upper bound for `t`
       * @return Approximate t value for which `calcBezier(t) = x`
       */
      private fun binarySubdivide(x: Float, left: Float, right: Float): Float {
         var l = left
         var r = right
         var currentX: Float
         var currentT: Float
         var i = 0
         do {
            currentT = l + (r - l) / 2
            currentX = calcBezier(currentT, xS, xE) - x
            if (currentX > 0) {
               r = currentT
            } else l = currentT
         } while (abs(currentX) > SUBDIVISION_PRECISION && ++i < SUBDIVISION_MAX_ITERATIONS)
         return currentT
      }

      /**
       * When easing, the `t` value from [Easing] is our `x`.
       */
      override fun ease(t: Float) =
         if (sampleValues == null) t else calcBezier(getTForX(t), yS, yE) // Short-circuit linear bezier

      override fun toString() = "cubicBezier($xS, $yS, $xE, $yE)"

      companion object {
         // Should be enough for simple precision
         private const val NEWTON_ITERATIONS = 2
         private const val NEWTON_MIN_SLOPE = 0.02F

         // Roughly enough for simple precision
         private const val SUBDIVISION_PRECISION = 1E-6F
         // Maximum resolution of 3.9E-3 * (sample-gap-size)
         private const val SUBDIVISION_MAX_ITERATIONS = 8

         // 10 gaps
         private const val SAMPLE_TABLE_SIZE = 11
         private const val SAMPLE_STEP_SIZE = 1F / (SAMPLE_TABLE_SIZE - 1)
      }
   }
}

