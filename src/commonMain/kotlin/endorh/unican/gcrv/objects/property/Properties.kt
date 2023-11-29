package endorh.unican.gcrv.objects.property

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.Checkbox
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.onToggle
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.renderers.OptionIdxPicker
import endorh.unican.gcrv.line_algorithms.renderers.OptionalOptionIdxPicker
import endorh.unican.gcrv.line_algorithms.ui.*
import endorh.unican.gcrv.transformations.TransformProperty
import endorh.unican.gcrv.types.ColorSerializer
import endorh.unican.gcrv.types.Vec2fSerializer
import endorh.unican.gcrv.types.Vec2iSerializer
import endorh.unican.gcrv.ui2.ColorField
import endorh.unican.gcrv.util.D
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
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
typealias transform = TransformProperty
typealias option<T> = OptionProperty<T>
typealias nullOption<T> = NullableOptionProperty<T>

fun <T: PropertyNode> list(vararg elems: T, factory: () -> T) =
   PropertyList(factory, elems.toList())
fun <T : PropertyNode> list(showExpanded: Boolean, vararg elems: T, factory: () -> T) =
   PropertyList(factory, elems.toList(), showExpanded)


@Serializable(BoolProperty.Serializer::class)
class BoolProperty(value: Boolean) : AnimProperty<Boolean>(value) {
   override fun UiEditorScope<Boolean>.editor(value: Boolean, modifier: UiScope.() -> Unit) {
      Checkbox(value) {
         this.modifier.onToggle { commitEdit(it) }.apply {
            fillColor = colors.primary.mix(editorTint, 0.8F)
         }
         modifier()
      }
   }
   object Serializer : AnimProperty.Serializer<Boolean, BoolProperty>(::BoolProperty, Boolean.serializer())
}

@Serializable(StringProperty.Serializer::class)
class StringProperty(value: String) : AnimProperty<String>(value) {
   override val availableInterpolators: List<PropertyInterpolator<String>> =
      listOf(StringMixInterpolator)
   override fun UiEditorScope<String>.editor(value: String, modifier: UiScope.() -> Unit) {
      StringField(value, { commitEdit(it) }, "edit/$name/str", editorTint) {
         modifier()
      }
   }

   open class StringMixInterpolator(private val random: Random) : PropertyInterpolator<String> {
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

      companion object : StringMixInterpolator(Random(0))
   }

   object Serializer : AnimProperty.Serializer<String, StringProperty>(::StringProperty, String.serializer())
}

@Serializable(IntProperty.Serializer::class)
class IntProperty(value: Int) : AnimProperty<Int>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Int>> =
      listOf(LinearInterpolator)

   override fun UiEditorScope<Int>.editor(value: Int, modifier: UiScope.() -> Unit) {
      IntField(value, { commitEdit(it) }, "edit/$name/int", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Int> {
      override fun interpolate(a: Int, b: Int, t: Float): Int =
         (a + (b - a) * t).I
   }

   object Serializer : AnimProperty.Serializer<Int, IntProperty>(::IntProperty, Int.serializer())
}

@Serializable(FloatProperty.Serializer::class)
class FloatProperty(value: Float) : AnimProperty<Float>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Float>> =
      listOf(LinearInterpolator)
   override fun UiEditorScope<Float>.editor(value: Float, modifier: UiScope.() -> Unit) {
      FloatField(value, { commitEdit(it) }, "edit/$name/float", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Float> {
      override fun interpolate(a: Float, b: Float, t: Float): Float =
         a + (b - a) * t
   }

   object Serializer : AnimProperty.Serializer<Float, FloatProperty>(::FloatProperty, Float.serializer())
}

@Serializable(DoubleProperty.Serializer::class)
class DoubleProperty(value: Double) : AnimProperty<Double>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Double>> =
      listOf(LinearInterpolator)
   override fun UiEditorScope<Double>.editor(value: Double, modifier: UiScope.() -> Unit) {
      FloatField(value.toFloat(), { commitEdit(it.D) }, "edit/$name/double", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Double> {
      override fun interpolate(a: Double, b: Double, t: Float): Double =
         a + (b - a) * t
   }

   object Serializer : AnimProperty.Serializer<Double, DoubleProperty>(::DoubleProperty, Double.serializer())
}

@Serializable(Vec2iProperty.Serializer::class)
class Vec2iProperty(value: Vec2i) : AnimProperty<Vec2i>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Vec2i>> =
      listOf(LinearInterpolator)
   override fun UiEditorScope<Vec2i>.editor(value: Vec2i, modifier: UiScope.() -> Unit) {
      Vec2iField(value, { commitEdit(it) }, "edit/$name/vec2i", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Vec2i> {
      override fun interpolate(a: Vec2i, b: Vec2i, t: Float): Vec2i =
         Vec2i((a.x + (b.x - a.x) * t).I, (a.y + (b.y - a.y) * t).I)
   }

   object Serializer : AnimProperty.Serializer<Vec2i, Vec2iProperty>(::Vec2iProperty, Vec2iSerializer)
}

@Serializable(Vec2fProperty.Serializer::class)
class Vec2fProperty(value: Vec2f) : AnimProperty<Vec2f>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Vec2f>> =
      listOf(LinearInterpolator)
   override fun UiEditorScope<Vec2f>.editor(value: Vec2f, modifier: UiScope.() -> Unit) {
      Vec2fField(value, { commitEdit(it) }, "edit/$name/vec2f", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Vec2f> {
      override fun interpolate(a: Vec2f, b: Vec2f, t: Float): Vec2f =
         Vec2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
   }

   object Serializer : AnimProperty.Serializer<Vec2f, Vec2fProperty>(::Vec2fProperty, Vec2fSerializer)
}

@Serializable(ColorProperty.Serializer::class)
class ColorProperty(value: Color) : AnimProperty<Color>(value) {
   override val availableInterpolators: List<PropertyInterpolator<Color>> =
      listOf(HsvInterpolator, LinearInterpolator)

   override fun UiEditorScope<Color>.editor(value: Color, modifier: UiScope.() -> Unit) {
      ColorField(value, { commitEdit(it) }, true, "edit/$name/color", editorTint) { modifier() }
   }

   object LinearInterpolator : PropertyInterpolator<Color> {
      override fun interpolate(a: Color, b: Color, t: Float) = Color(
         a.r + (b.r - a.r) * t,
         a.g + (b.g - a.g) * t,
         a.b + (b.b - a.b) * t,
         a.a + (b.a - a.a) * t
      )
   }

   object HsvInterpolator : PropertyInterpolator<Color> {
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

   object Serializer : AnimProperty.Serializer<Color, ColorProperty>(::ColorProperty, ColorSerializer)
}

class OptionProperty<T: Any>(val values: List<T>, value: T) : AnimProperty<T>(value) {
   init {
      if (value !in values) throw IllegalArgumentException("Value $value not in value-set: $values")
   }

   override fun UiEditorScope<T>.editor(value: T, modifier: UiScope.() -> Unit) {
      OptionIdxPicker(values, values.indexOf(value), { commitEdit(values[it]) }, "edit/$name/option", modifier = modifier)
   }
}

class NullableOptionProperty<T>(
   val values: List<T & Any>,
   value: T?
) : AnimProperty<T?>(value) {
   override fun UiEditorScope<T?>.editor(value: T?, modifier: UiScope.() -> Unit) {
      OptionalOptionIdxPicker(values, values.indexOf(value), {
         commitEdit(if (it != null) values[it] else null)
      }, "edit/$name/option?", modifier = modifier)
   }
}