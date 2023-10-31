package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.TextField
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.onChange
import endorh.unican.gcrv.line_algorithms.ui.FloatField
import endorh.unican.gcrv.line_algorithms.ui.IntField
import endorh.unican.gcrv.line_algorithms.ui.LineStyleEditor
import endorh.unican.gcrv.line_algorithms.ui.Vec2iField
import kotlin.reflect.KProperty

abstract class ObjectProperty<T>(value: T) : MutableStateValue<T>(value) {
   // operator fun getValue(obj: Object2D, property: KProperty<*>): T = value
   // operator fun setValue(obj: Object2D, property: KProperty<*>, value: T) {
   //    this.value = value
   // }
   open val isSimpleEditor = true
   abstract fun UiScope.editor(modifier: UiScope.() -> Unit = {})
}

class StringProperty(value: String) : ObjectProperty<String>(value) {
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      TextField(value) { this.modifier.onChange { value = it }; modifier() }
   }
}

class IntProperty(value: Int) : ObjectProperty<Int>(value) {
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      IntField(value, { value = it }) { modifier() }
   }
}

class FloatProperty(value: Float) : ObjectProperty<Float>(value) {
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      FloatField(value, { value = it }) { modifier() }
   }
}

class DoubleProperty(value: Double) : ObjectProperty<Double>(value) {
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      FloatField(value.toFloat(), { value = it.toDouble() }) { modifier() }
   }
}

class Vec2iProperty(value: Vec2i) : ObjectProperty<Vec2i>(value) {
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      Vec2iField(value, { value = it }) { modifier() }
   }
}