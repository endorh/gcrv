package endorh.unican.gcrv.objects.property

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.animation.*
import kotlin.js.JsName
import kotlin.jvm.JvmInline

@JsName("createPropertyMap")
fun PropertyMap(): PropertyMap = PropertyMapImpl()

internal val PropertyNode.allProperties: Sequence<AnimProperty<*>> get() = when (this) {
   is AnimProperty<*> -> sequenceOf(this)
   is CompoundAnimProperty -> properties.allProperties
   is PropertyList<*> -> entries.asSequence().flatMap { it.allProperties }
}

sealed interface PropertyMap : Map<String, PropertyNode> {
   val sortedProperties: Sequence<PropertyNode> get() =
      values.asSequence().sortedBy { it.priority }
   val allProperties: Sequence<AnimProperty<*>> get() =
      values.asSequence().flatMap { it.allProperties }
}

@JvmInline value class PropertyMapImpl(
   val map: MutableMap<String, PropertyNode> = mutableMapOf()
) : PropertyMap, Map<String, PropertyNode> by map

internal fun PropertyHolder.register(property: PropertyNode) {
   val props = properties as PropertyMapImpl
   if (property.name in props)
      throw IllegalStateException("Property ${property.name} already registered")
   props.map[property.name] = property
}

interface PropertyHolder {
   val properties: PropertyMap
   val timeLine: MutableStateValue<TimeLine?>
}

sealed interface PropertyNode {
   val holder: PropertyHolder?
   val name: String
   val priority: Int

   fun onChange(block: () -> Unit)
   fun clearChangeListeners()
}

infix fun <N: PropertyNode> N.priority(priority: Int): N = apply {
   setPriority(priority)
}

internal fun PropertyNode.init(name: String, holder: PropertyHolder?) {
   when (this) {
      is AnimProperty<*> -> {
         this.name = name
         this.holder = holder
      }
      is CompoundAnimProperty -> {
         this.name = name
         this.holder = holder
      }
      is PropertyList<*> -> {
         this.name = name
         this.holder = holder
      }
   }
}
internal fun PropertyNode.setPriority(priority: Int) {
   when (this) {
      is AnimProperty<*> -> this.priority = priority
      is CompoundAnimProperty -> this.priority = priority
      is PropertyList<*> -> this.priority = priority
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
