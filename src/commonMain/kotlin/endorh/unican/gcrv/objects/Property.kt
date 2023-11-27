package endorh.unican.gcrv.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.KeyFrame
import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.animation.TimeStamp
import endorh.unican.gcrv.line_algorithms.renderers.OptionPicker
import endorh.unican.gcrv.line_algorithms.renderers.OptionalOptionPicker
import endorh.unican.gcrv.line_algorithms.ui.*
import endorh.unican.gcrv.ui2.ColorField
import endorh.unican.gcrv.util.D
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import endorh.unican.gcrv.util.toTitleCase
import kotlin.js.JsName
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.reflect.KProperty

@JsName("createPropertyMap")
fun PropertyMap(): PropertyMap = PropertyMapImpl()

internal val PropertyNode.allProperties: Sequence<AnimProperty<*>> get() = when (this) {
   is AnimProperty<*> -> sequenceOf(this)
   is CompoundAnimProperty -> properties.allProperties
}

sealed interface PropertyMap : Map<String, PropertyNode> {
   val allProperties: Sequence<AnimProperty<*>> get() =
      values.asSequence().flatMap { it.allProperties }
}

@JvmInline value class PropertyMapImpl(
   val map: MutableMap<String, PropertyNode> = mutableMapOf()
) : PropertyMap, Map<String, PropertyNode> by map

internal fun PropertyMap.register(property: AnimProperty<*>) {
   this as PropertyMapImpl
   if (property.name in map) throw IllegalStateException("Property ${property.name} already registered")
   map[property.name] = property
}
internal fun PropertyMap.register(holder: CompoundAnimProperty) {
   this as PropertyMapImpl
   if (holder.name in map) throw IllegalStateException("Property ${holder.name} already registered")
   map[holder.name] = holder
}

sealed interface PropertyNode {
   val name: String
}

abstract class CompoundAnimProperty : PropertyNode {
   override lateinit var name: String
   val properties: PropertyMap = PropertyMap()

   open val showExpanded get() = true

   fun <T> AnimProperty<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): AnimProperty<T> {
      name = property.name
      properties.register(this)
      return this
   }
   fun <T> CompoundAnimProperty.provideDelegate(thisRef: Any?, property: KProperty<*>): CompoundAnimProperty {
      name = property.name
      properties.register(this)
      return this
   }
}

abstract class AnimProperty<T>(defValue: T) : MutableState(), PropertyNode {
   override lateinit var name: String

   var isUnique = false
      private set
   /**
    * Mark as unique.
    * Unique properties won't be editable when multiple objects are selected
    */
   fun unique() = apply { isUnique = true }

   private val stateListeners = mutableListOf<(T) -> Unit>()
   val keyFrames = KeyFrameList<T>()
   val timeLine = mutableStateOf<TimeLine?>(null).onChange {
      it?.currentTime?.onChange { notifyListeners(valueForTime(it)) }
   }

   private var plainValue = defValue
   var value: T
      get() = timeLine.value?.let { timeLine ->
         valueForTime(timeLine.currentTime.value)
      } ?: plainValue
      set(value) {
         timeLine.value?.takeIf { keyFrames.isNotEmpty() } ?.let { timeLine ->
            keyFrames.set(KeyFrame(timeLine.currentTime.value, value, defaultInterpolator))
         } ?: run { plainValue = value }
         notifyListeners(value)
      }

   open val availableInterpolators: List<PropertyInterpolator<T>> = emptyList()

   fun valueForTime(time: TimeStamp) = keyFrames.valueForTime(time) ?: plainValue
   fun insertKeyFrame(time: TimeStamp) {
      keyFrames.set(KeyFrame(time, valueForTime(time), defaultInterpolator))
   }

   val defaultInterpolator get() = availableInterpolators.firstOrNull() ?: PropertyInterpolator.None()

   private fun notifyListeners(newState: T) {
      stateChanged()
      for (listener in stateListeners) listener(newState)
   }

   fun use(surface: UiSurface): T = value.also {
      usedBy(surface)
   }
   fun onChange(block: (T) -> Unit) = apply {
      stateListeners += block
   }

   operator fun UiScope.getValue(thisRef: Any?, property: KProperty<*>) = use(surface)
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
   operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
      this.value = value
   }

   override fun toString() = "AnimProperty($value)"

   open val animatedEditorTint = Color("FF42FF80")
   open val editorTint: Color? get() = if (keyFrames.keyFrames.isNotEmpty()) animatedEditorTint else null
   abstract fun UiEditorScope<T>.editor(value: T, modifier: UiScope.() -> Unit = {})
   open fun UiScope.editor(
      selected: Collection<AnimProperty<T>> = emptyList(),
      onFocus: () -> Unit = {},
      modifier: UiModifier.() -> UiModifier = { this }
   ) {
      use(surface)
      LabeledField(name.toTitleCase(), modifier) {
         this.modifier.onClick {
            onFocus()
         }
         UiEditorScope.Impl(this@AnimProperty, selected, this).editor(value, it)
      }
   }
}

interface UiEditorScope<T> : UiScope {
   fun commitEdit(value: T)

   class Impl<T>(
      private val property: AnimProperty<T>,
      private val properties: Collection<AnimProperty<T>>,
      private val scope: UiScope
   ) : UiEditorScope<T>, UiScope by scope {
      override fun commitEdit(value: T) {
         property.value = value
         for (it in properties)
            if (it::class == property::class)
               it.value = value
      }
   }
}

interface PropertyInterpolator<T> {
   fun interpolate(a: T, b: T, t: Float): T

   companion object {
      @Suppress("UNCHECKED_CAST")
      fun <T> None() = None as PropertyInterpolator<T>
   }

   object None : PropertyInterpolator<Any?> {
      override fun interpolate(a: Any?, b: Any?, t: Float): Any? =
         if (t < 1F) a else b
   }
}

class BoolProperty(value: Boolean) : AnimProperty<Boolean>(value) {
   override fun UiEditorScope<Boolean>.editor(value: Boolean, modifier: UiScope.() -> Unit) {
      Checkbox(value) {
         this.modifier.onToggle { commitEdit(it) }.apply {
            fillColor = colors.primary.mix(editorTint, 0.8F)
         }
         modifier()
      }
   }
}

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
}

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
}

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
}

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
}

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
}

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
}

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
         a.a + (b.a - a.a) * t)
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
}

class OptionProperty<T: Any>(val values: List<T>, value: T) : AnimProperty<T>(value) {
   init {
      if (value !in values) throw IllegalArgumentException("Value $value not in value-set: $values")
   }

   override fun UiEditorScope<T>.editor(value: T, modifier: UiScope.() -> Unit) {
      OptionPicker(values, values.indexOf(value), { commitEdit(values[it]) }, modifier, "edit/$name/option")
   }
}

class NullableOptionProperty<T>(
   val values: List<T & Any>,
   value: T?
) : AnimProperty<T?>(value) {

   override fun UiEditorScope<T?>.editor(value: T?, modifier: UiScope.() -> Unit) {
      OptionalOptionPicker(values, values.indexOf(value), {
         commitEdit(if (it != null) values[it] else null)
      }, modifier, "edit/$name/option?")
   }
}