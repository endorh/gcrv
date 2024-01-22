package endorh.unican.gcrv.scene.property

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.Checkbox
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.onToggle
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.OptionIdxPicker
import endorh.unican.gcrv.renderers.OptionalOptionIdxPicker
import endorh.unican.gcrv.scene.CubicSplineStyleProperty
import endorh.unican.gcrv.scene.FillStyleProperty
import endorh.unican.gcrv.scene.LineStyleProperty
import endorh.unican.gcrv.scene.PointStyleProperty
import endorh.unican.gcrv.transformations.TransformProperty
import endorh.unican.gcrv.serialization.ColorSerializer
import endorh.unican.gcrv.serialization.Vec2fSerializer
import endorh.unican.gcrv.serialization.Vec2iSerializer
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.D
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import endorh.unican.gcrv.util.toVec2f
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import kotlin.random.Random

typealias bool = BoolProperty
typealias string = StringProperty
typealias int = IntProperty
typealias float = FloatProperty
typealias double = DoubleProperty
typealias vec2i = Vec2iProperty
typealias vec2f = Vec2fProperty
typealias color = ColorProperty
typealias lineStyle = LineStyleProperty
typealias pointStyle = PointStyleProperty
typealias cubicSplineStyle = CubicSplineStyleProperty
typealias fillStyle = FillStyleProperty
typealias transform = TransformProperty
typealias option<T> = OptionProperty<T>
typealias nullOption<T> = NullableOptionProperty<T>

inline fun <reified T: Any> option(values: List<T>, value: T) =
   option(values, value, serializer<T>())
inline fun <reified T> nullOption(values: List<T & Any>, value: T?) =
   nullOption(values, value, serializer<T?>())

fun <S, T: PropertyNode<S>> list(vararg elems: T, factory: () -> T) =
   PropertyList(factory, elems.toList())
fun <S, T: PropertyNode<S>> list(showExpanded: Boolean, vararg elems: T, factory: () -> T) =
   PropertyList(factory, elems.toList(), showExpanded)


// @Serializable(BoolProperty.Serializer::class)
class BoolProperty(value: Boolean) : AnimProperty<Boolean>(value, Boolean.serializer()) {
   override fun UiEditorScope<Boolean>.editor(value: Boolean, modifier: UiScope.() -> Unit) {
      Checkbox(value) {
         this.modifier.onToggle { commitEdit(it) }.apply {
            fillColor = colors.primary.mix(editorTint, 0.8F)
         }
         modifier()
      }
   }
   // object Serializer : AnimProperty.Serializer<Boolean, BoolProperty>(::BoolProperty, Boolean.serializer())
}

// @Serializable(StringProperty.Serializer::class)
class StringProperty(value: String) : AnimProperty<String>(value, String.serializer()) {
   override val availableDrivers: List<PropertyDriver<String>> =
      listOf(StringMixDriver)
   override fun UiEditorScope<String>.editor(value: String, modifier: UiScope.() -> Unit) {
      StringField(value, { commitEdit(it) }, "edit/$name/str", editorTint) {
         modifier()
      }
   }

   open class StringMixDriver(private val random: Random) : PropertyDriver<String> {
      override val name = "mix"
      override fun interpolate(a: String, b: String, t: Float): String {
         var ai = 0
         var bi = 0
         val f = a.length.F / b.length.F
         val sb = StringBuilder()
         for (i in 0 until a.length + b.length) {
            if (ai < a.length) {
               if (random.nextFloat() > t / f) {
                  if (random.nextFloat() > t) sb.append(a[ai])
                  ai++
                  continue
               }
            }
            if (bi < b.length) {
               if (random.nextFloat() < t) sb.append(b[bi])
               bi++
            }
         }
         return sb.toString()
      }
      companion object : StringMixDriver(Random(0))
   }

   // object Serializer : AnimProperty.Serializer<String, StringProperty>(::StringProperty, String.serializer())
}

// @Serializable(IntProperty.Serializer::class)
class IntProperty(value: Int) : AnimProperty<Int>(value, Int.serializer()) {
   override val availableDrivers: List<PropertyDriver<Int>> =
      listOf(LinearDriver)

   override fun UiEditorScope<Int>.editor(value: Int, modifier: UiScope.() -> Unit) {
      IntField(value, { commitEdit(it) }, "edit/$name/int", editorTint) { modifier() }
   }

   object LinearDriver : PropertyDriver<Int> {
      override val name = "linear"
      override fun interpolate(a: Int, b: Int, t: Float): Int =
         (a + (b - a) * t).I
   }

   // object Serializer : AnimProperty.Serializer<Int, IntProperty>(::IntProperty, Int.serializer())
}

// @Serializable(FloatProperty.Serializer::class)
class FloatProperty(value: Float) : AnimProperty<Float>(value, Float.serializer()) {
   override val availableDrivers: List<PropertyDriver<Float>> =
      listOf(LinearDriver)
   override fun UiEditorScope<Float>.editor(value: Float, modifier: UiScope.() -> Unit) {
      FloatField(value, { commitEdit(it) }, "edit/$name/float", editorTint) { modifier() }
   }

   object LinearDriver : PropertyDriver<Float> {
      override val name = "linear"
      override fun interpolate(a: Float, b: Float, t: Float): Float =
         a + (b - a) * t
   }

   // object Serializer : AnimProperty.Serializer<Float, FloatProperty>(::FloatProperty, Float.serializer())
}

// @Serializable(DoubleProperty.Serializer::class)
class DoubleProperty(value: Double) : AnimProperty<Double>(value, Double.serializer()) {
   override val availableDrivers: List<PropertyDriver<Double>> =
      listOf(LinearDriver)
   override fun UiEditorScope<Double>.editor(value: Double, modifier: UiScope.() -> Unit) {
      FloatField(value.toFloat(), { commitEdit(it.D) }, "edit/$name/double", editorTint) { modifier() }
   }

   object LinearDriver : PropertyDriver<Double> {
      override val name = "linear"
      override fun interpolate(a: Double, b: Double, t: Float): Double =
         a + (b - a) * t
   }

   // object Serializer : AnimProperty.Serializer<Double, DoubleProperty>(::DoubleProperty, Double.serializer())
}

// @Serializable(Vec2iProperty.Serializer::class)
class Vec2iProperty(value: Vec2i) : AnimProperty<Vec2i>(value, Vec2iSerializer), GeometricProperty2D {
   override val availableDrivers: List<PropertyDriver<Vec2i>> =
      listOf(LinearDriver)
   override fun UiEditorScope<Vec2i>.editor(value: Vec2i, modifier: UiScope.() -> Unit) {
      Vec2iField(value, { commitEdit(it) }, "edit/$name/vec2i", editorTint) { modifier() }
   }

   override val geometricCenter get() = value.toVec2f()
   override fun applyTransform(transform: Transform2D) {
      value = transform * value
   }

   object LinearDriver : PropertyDriver<Vec2i> {
      override val name = "linear"
      override fun interpolate(a: Vec2i, b: Vec2i, t: Float): Vec2i =
         Vec2i((a.x + (b.x - a.x) * t).I, (a.y + (b.y - a.y) * t).I)
   }

   // object Serializer : AnimProperty.Serializer<Vec2i, Vec2iProperty>(::Vec2iProperty, Vec2iSerializer)
}

// @Serializable(Vec2fProperty.Serializer::class)
class Vec2fProperty(value: Vec2f) : AnimProperty<Vec2f>(value, Vec2fSerializer), GeometricProperty2D {
   override val availableDrivers: List<PropertyDriver<Vec2f>> =
      listOf(LinearDriver)
   override fun UiEditorScope<Vec2f>.editor(value: Vec2f, modifier: UiScope.() -> Unit) {
      Vec2fField(value, { commitEdit(it) }, "edit/$name/vec2f", editorTint) { modifier() }
   }
   override val geometricCenter get() = value
   override fun applyTransform(transform: Transform2D) {
      value = transform * value
   }

   object LinearDriver : PropertyDriver<Vec2f> {
      override val name = "linear"
      override fun interpolate(a: Vec2f, b: Vec2f, t: Float): Vec2f =
         Vec2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
   }

   // object Serializer : AnimProperty.Serializer<Vec2f, Vec2fProperty>(::Vec2fProperty, Vec2fSerializer)
}

// @Serializable(ColorProperty.Serializer::class)
class ColorProperty(value: Color) : AnimProperty<Color>(value, ColorSerializer) {
   override val availableDrivers: List<PropertyDriver<Color>> =
      listOf(HsvLinearDriver, LinearDriver)

   override fun UiEditorScope<Color>.editor(value: Color, modifier: UiScope.() -> Unit) {
      ColorField(value, { commitEdit(it) }, true, "edit/$name/color", editorTint) { modifier() }
   }

   object LinearDriver : PropertyDriver<Color> {
      override val name = "linear"
      override fun interpolate(a: Color, b: Color, t: Float) = Color(
         a.r + (b.r - a.r) * t,
         a.g + (b.g - a.g) * t,
         a.b + (b.b - a.b) * t,
         a.a + (b.a - a.a) * t
      )
   }

   object HsvLinearDriver : PropertyDriver<Color> {
      override val name = "hsv"
      override fun interpolate(a: Color, b: Color, t: Float): Color {
         val aHsv = a.toHsv()
         val bHsv = b.toHsv()
         val d = bHsv.h - aHsv.h
         return Color.Hsv(
            if (d > 180) (aHsv.h + 360F + (d - 360F) * t) % 360F
            else if (d < -180) (aHsv.h + (d + 360F) * t) % 360F
            else aHsv.h + d * t,
            aHsv.s + (bHsv.s - aHsv.s) * t,
            aHsv.v + (bHsv.v - aHsv.v) * t
         ).toSrgb(a = a.a + (b.a - a.a) * t)
      }
   }

   // object Serializer : AnimProperty.Serializer<Color, ColorProperty>(::ColorProperty, ColorSerializer)
}

@OptIn(InternalSerializationApi::class)
class OptionProperty<T: Any>(
   val values: List<T>, value: T,
   serializer: KSerializer<T>
) : AnimProperty<T>(value, serializer) {
   init {
      if (value !in values) throw IllegalArgumentException("Value $value not in value-set: $values")
   }

   override fun UiEditorScope<T>.editor(value: T, modifier: UiScope.() -> Unit) {
      OptionIdxPicker(values, values.indexOf(value), { commitEdit(values[it]) }, "edit/$name/option", modifier = modifier)
   }
}

class NullableOptionProperty<T>(
   val values: List<T & Any>, value: T?,
   serializer: KSerializer<T?>
) : AnimProperty<T?>(value, serializer) {
   override fun UiEditorScope<T?>.editor(value: T?, modifier: UiScope.() -> Unit) {
      OptionalOptionIdxPicker(values, values.indexOf(value), {
         commitEdit(if (it != null) values[it] else null)
      }, "edit/$name/option?", modifier = modifier)
   }
}