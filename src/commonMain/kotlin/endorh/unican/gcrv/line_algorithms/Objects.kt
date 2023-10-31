package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.ui.LabeledField
import endorh.unican.gcrv.line_algorithms.ui.LineStyleProperty
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Object2DStack {
   val objects = mutableStateListOf<Object2D>()
}

abstract class Object2D(val type: Object2DType<out Object2D> = Object2DType("Object")) {
   private val mutableProperties = mutableMapOf<String, ObjectProperty<*>>()
   val properties: Map<String, ObjectProperty<*>> get() = mutableProperties

   var name: String by string(type.generateName())
   abstract val wireLines: List<Line2D>

   @Suppress("UNCHECKED_CAST")
   operator fun <T> get(prop: KProperty<T>) = properties[prop.name] as? ObjectProperty<T>
      ?: throw IllegalArgumentException("Property ${prop.name} is not an ObjectProperty")

   protected open fun UiModifier.editorModifier() = this
   open fun UiScope.editor() {
      Column(scopeName = "Editor/$name") {
         modifier
            .width(Grow.Std).margin(2.dp).padding(4.dp)
            .backgroundColor(colors.backgroundVariant)
            .editorModifier()
         for ((name, prop) in properties) with(prop) {
            if (isSimpleEditor) {
               LabeledField(name.replaceFirstChar {
                  if (it.isLowerCase()) it.titlecase() else it.toString()
               }) {
                  editor(it)
               }
            } else editor()
         }
      }
   }

   fun onPropertyChange(block: () -> Unit) = properties.values.forEach { it.onChange { block() } }

   class ObjectPropertyDelegateProvider<T>(val prop: ObjectProperty<T>) :
      PropertyDelegateProvider<Object2D, ObjectPropertyDelegateProvider.PropertyDelegate<T>> {
      override fun provideDelegate(thisRef: Object2D, property: KProperty<*>): PropertyDelegate<T> {
         thisRef.mutableProperties[property.name] = prop
         return PropertyDelegate(prop)
      }

      class PropertyDelegate<T>(val prop: ObjectProperty<T>) : ReadWriteProperty<Object2D, T> {
         override fun getValue(thisRef: Object2D, property: KProperty<*>) = prop.value
         override fun setValue(thisRef: Object2D, property: KProperty<*>, value: T) {
            prop.value = value
         }
      }
   }

   protected fun <T> prop(value: ObjectProperty<T>) = ObjectPropertyDelegateProvider(value)
   protected fun string(value: String) = prop(StringProperty(value))
   protected fun int(value: Int) = prop(IntProperty(value))
   protected fun float(value: Float) = prop(FloatProperty(value))
   protected fun double(value: Double) = prop(DoubleProperty(value))
   protected fun vec2i(value: Vec2i) = prop(Vec2iProperty(value))
   protected fun lineStyle(value: LineStyle) = prop(LineStyleProperty(value))
}

open class Object2DType<T : Object2D>(val name: String) {
   open fun generateName() = name
}

class LineObject2D(
   start: Vec2i,
   end: Vec2i,
   style: LineStyle = LineStyle(Color.WHITE)
) : Object2D(Type) {
   val start by vec2i(start)
   val end by vec2i(end)
   val style by lineStyle(style)

   override val wireLines: List<Line2D> get() = listOf(Line2D(start, end, style))

   object Type : Object2DType<LineObject2D>("Line") {
      private var lineCount = 0
      override fun generateName() = "Line ${++lineCount}"
   }
}