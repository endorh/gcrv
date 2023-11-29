package endorh.unican.gcrv.animation

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.math.clamp
import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.Line2D
import endorh.unican.gcrv.line_algorithms.LineStyle
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.line_algorithms.renderers.PresentableObject
import endorh.unican.gcrv.line_algorithms.renderers.line.BresenhamRendererBreadthAntiAlias
import endorh.unican.gcrv.line_algorithms.ui.LabeledFloatField
import endorh.unican.gcrv.line_algorithms.ui.LabeledVec2fField
import endorh.unican.gcrv.types.Vec2fSerializer
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.times
import endorh.unican.gcrv.util.toTitleCase
import endorh.unican.gcrv.util.toVec2i
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlin.math.*
import kotlin.reflect.KProperty

open class EasingType<T: Easing>(val name: String, val factory: () -> T) : PresentableObject, KSerializer<T> {
   override val displayName get() = name
   private val controlPoints = factory().controlPoints

   override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
      controlPoints.forEachIndexed { i, p ->
         element(p.name, Vec2fSerializer.descriptor)
      }
   }
   override fun serialize(encoder: Encoder, value: T) {
      encoder.beginStructure(descriptor).apply {
         controlPoints.forEachIndexed { i, p ->
            encodeSerializableElement(descriptor, i, Vec2fSerializer, p.value)
         }
      }
   }
   override fun deserialize(decoder: Decoder): T {
      return decoder.decodeStructure(descriptor) {
         val pointValues = arrayOfNulls<Vec2f>(controlPoints.size)
         while (true) {
            val index = decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            if (index > pointValues.lastIndex) throw SerializationException("Invalid control point index: $index")
            pointValues[index] = decodeSerializableElement(descriptor, index, Vec2fSerializer, pointValues[index])
         }
         val e = factory()
         e.controlPoints.forEachIndexed { i, p ->
            p.value = pointValues[i] ?: throw SerializationException("Missing control point: ${p.name}")
         }
         e
      }
   }
}

val EasingTypes = mutableListOf(
   Easing.Linear.Type,
   Easing.EaseIn.Type,
   Easing.EaseOut.Type,
   Easing.EaseInOut.Type,
   Easing.EaseCircIn.Type,
   Easing.EaseCircOut.Type,
   Easing.EaseCircInOut.Type,
   Easing.EaseCubicIn.Type,
   Easing.EaseCubicOut.Type,
   Easing.EaseCubicInOut.Type,
   Easing.EaseElasticIn.Type,
   Easing.CubicBezier.Type,
)

class ControlPoint(
   value: Vec2f = Vec2f.ZERO,
   val min: Vec2f = Vec2f(0F, Float.NEGATIVE_INFINITY),
   val max: Vec2f = Vec2f(1F, Float.POSITIVE_INFINITY),
   var onChange: () -> Unit = {}
): MutableState() {
   var value: Vec2f = value
      set(value) {
         field = Vec2f(value.x.clamp(min.x, max.x), value.y.clamp(min.y, max.y))
         stateChanged()
         onChange()
      }
   lateinit var name: String
   val isDualName get() = '_' in name
   val dualNames get() = name.substringBefore('_') to name.substringAfter('_')

   operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
   operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Vec2f) {
      this.value = value
   }

   fun UiScope.editor() {
      usedBy(surface)
      if (isDualName) {
         val (x, y) = dualNames
         LabeledFloatField(x.toTitleCase(), value.x, { value = Vec2f(it, value.y) })
         LabeledFloatField(y.toTitleCase(), value.y, { value = Vec2f(value.x, it) })
      } else {
         LabeledVec2fField(name.toTitleCase(), value, { value = it })
      }
   }

   fun use(surface: UiSurface) = apply { usedBy(surface) }
}

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(PolymorphicSerializer::class)
@Polymorphic
abstract class Easing(val type: EasingType<*>) {
   val name: String get() = type.name
   private val mutableControlPoints = mutableListOf<ControlPoint>()
   val controlPoints: List<ControlPoint> get() = mutableControlPoints

   protected fun control(
      value: Vec2f, min: Vec2f = Vec2f(0F, Float.NEGATIVE_INFINITY),
      max: Vec2f = Vec2f(1F, Float.POSITIVE_INFINITY)
   ) = ControlPoint(value, min, max, onChange = { controlsUpdated() }).also {
      mutableControlPoints.add(it)
   }

   operator fun ControlPoint.provideDelegate(thisRef: Easing, property: KProperty<*>): ControlPoint {
      name = property.name
      return this
   }

   open fun controlsUpdated() {}
   abstract fun ease(t: Float): Float
   open fun copy(): Easing {
      val copy = type.factory()
      copy.controlPoints.forEachIndexed { i, p ->
         p.value = controlPoints[i].value
      }
      return copy
   }

   open fun PixelRendererContext.drawGizmos(size: Int) {}

   @Serializable(Linear.Type::class) @SerialName("linear")
   object Linear : Easing(Type) {
      override fun ease(t: Float) = t
      object Type : EasingType<Linear>("Linear", { Linear })
   }

   @Serializable(EaseIn.Type::class) @SerialName("quad-in")
   object EaseIn : Easing(Type) {
      override fun ease(t: Float) = t * t
      object Type : EasingType<EaseIn>("Ease In", { EaseIn })
   }

   @Serializable(EaseOut.Type::class) @SerialName("quad-out")
   object EaseOut : Easing(Type) {
      override fun ease(t: Float) = t * (2 - t)
      object Type : EasingType<EaseOut>("Ease Out", { EaseOut })
   }

   @Serializable(EaseInOut.Type::class) @SerialName("quad-in-out")
   object EaseInOut : Easing(Type) {
      override fun ease(t: Float) = if (t < 0.5F) 2*t*t else (4 - 2*t) * t - 1
      object Type : EasingType<EaseInOut>("Ease In-Out", { EaseInOut })
   }

   @Serializable(EaseCircIn.Type::class) @SerialName("circ-in")
   object EaseCircIn : Easing(Type) {
      override fun ease(t: Float) = 1 - sqrt(1 - t*t)
      object Type : EasingType<EaseCircIn>("Circ Ease In", { EaseCircIn })
   }

   @Serializable(EaseCircOut.Type::class) @SerialName("circ-out")
   object EaseCircOut : Easing(Type) {
      override fun ease(t: Float) = sqrt(1 - (t - 1)*(t - 1))
      object Type : EasingType<EaseCircOut>("Circ Ease Out", { EaseCircOut })
   }

   @Serializable(EaseCircInOut.Type::class) @SerialName("circ-in-out")
   object EaseCircInOut : Easing(Type) {
      override fun ease(t: Float) = if (t < 0.5F) 0.5F * (1 - sqrt(1 - 4*t*t)) else 0.5F * (sqrt(-((2*t - 3)*(2*t - 1))) + 1)
      object Type : EasingType<EaseCircInOut>("Circ Ease In-Out", { EaseCircInOut })
   }

   @Serializable(EaseCubicIn.Type::class) @SerialName("cubic-in")
   object EaseCubicIn : Easing(Type) {
      override fun ease(t: Float) = t * t * t
      object Type : EasingType<EaseCubicIn>("Cubic Ease In", { EaseCubicIn })
   }

   @Serializable(EaseCubicOut.Type::class) @SerialName("cubic-out")
   object EaseCubicOut : Easing(Type) {
      override fun ease(t: Float) = 1 - (1 - t) * (1 - t) * (1 - t)
      object Type : EasingType<EaseCubicOut>("Cubic Ease Out", { EaseCubicOut })
   }

   @Serializable(EaseCubicInOut.Type::class) @SerialName("cubic-in-out")
   object EaseCubicInOut : Easing(Type) {
      override fun ease(t: Float) = if (t < 0.5F) 4*t*t*t else 1 + 4*(t - 1)*(t - 1)*(t - 1)
      object Type : EasingType<EaseCubicInOut>("Cubic Ease In-Out", { EaseCubicInOut })
   }

   @Serializable(EaseElasticIn.Type::class) @SerialName("elastic-in")
   class EaseElasticIn(
      amplitude: Float = 1F, period: Float = 0.3F
   ) : Easing(Type) {
      val period_amplitude by control(Vec2f(period, amplitude))

      val period get() = period_amplitude.x
      val amplitude get() = period_amplitude.y

      override fun PixelRendererContext.drawGizmos(size: Int) {
         with (BresenhamRendererBreadthAntiAlias) {
            render(Line2D(Vec2i(0, 0), (period_amplitude * size.F).toVec2i(), LineStyle(Color.LIGHT_GRAY)))
         }
      }

      override fun ease(t: Float): Float {
         if (t == 0F || t == 1F) return t
         val s = period / 4
         val tr = t - 1
         return -(amplitude * 2F.pow(10 * tr) * sin((tr - s) * (2 * PI) / period)).toFloat()
      }

      object Type : EasingType<EaseElasticIn>("Elastic Ease In", { EaseElasticIn() })
   }

   @Serializable(CubicBezier.Type::class) @SerialName("cubic-bezier")
   class CubicBezier(
      s: Vec2f = Vec2f(0.5F, 0F),
      e: Vec2f = Vec2f(0.5F, 1F)
   ) : Easing(Type) {
      var start by control(s)
      var end by control(e)

      override fun PixelRendererContext.drawGizmos(size: Int) {
         with (BresenhamRendererBreadthAntiAlias) {
            render(Line2D(Vec2i(0, 0), (start * size.F).toVec2i(), LineStyle(Color.LIGHT_GRAY)))
            render(Line2D(Vec2i(size - 1, size - 1), (end * size.F).toVec2i(), LineStyle(Color.LIGHT_GRAY)))
         }
      }

      var xS = s.x
      var yS = s.y
      var xE = e.x
      var yE = e.y

      private var sampleValues: FloatArray? = null

      init {
         controlsUpdated()
      }

      override fun controlsUpdated() {
         xS = start.x
         yS = start.y
         xE = end.x
         yE = end.y

         sampleValues = if (xS != yS || xE != yE) {
            calcSampleValues()
         } else null // Short-circuit linear bezier
      }

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
      private fun A(p1: Float, p2: Float) = 1 - 3 * p2 + 3 * p1

      /**
       * Degree 2 coefficient of a 1D cubic bezier polynomial.
       * @param p1 control point 1
       * @param p2 control point 2
       * @return `3p2 - 6p1`
       */
      private fun B(p1: Float, p2: Float) = 3 * p2 - 6 * p1

      /**
       * Degree 1 coefficient of a 1D cubic bezier polynomial.
       * @param p1 control point 1
       * @return `3p1`
       */
      private fun C(p1: Float) = 3 * p1

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
         val next = sampleValues!![currentIndex + 1]
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

         fun Linear() = CubicBezier(Vec2f(0.4F, 0.4F), Vec2f(0.6F, 0.6F))
      }

      object Type : EasingType<CubicBezier>("Cubic Bezier", { CubicBezier() })
   }
}