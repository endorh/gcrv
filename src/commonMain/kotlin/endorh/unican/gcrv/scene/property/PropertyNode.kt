package endorh.unican.gcrv.scene.property

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.animation.*
import endorh.unican.gcrv.serialization.Savable
import endorh.unican.gcrv.serialization.SavableSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.js.JsName
import kotlin.jvm.JvmInline

@JsName("createPropertyMap")
fun PropertyMap(): PropertyMap = PropertyMapImpl()

internal val PropertyNode<*>.allProperties: Sequence<AnimProperty<*>> get() = when (this) {
   is AnimProperty<*> -> sequenceOf(this)
   is CompoundAnimProperty -> properties.allProperties
   is PropertyList<*, *> -> entries.asSequence().flatMap { it.allProperties }
}

@Serializable
sealed interface PropertyMap : Map<String, PropertyNode<*>> {
   val sortedProperties: Sequence<PropertyNode<*>> get() =
      values.asSequence().sortedBy { it.priority }
   val allProperties: Sequence<AnimProperty<*>> get() =
      values.asSequence().flatMap { it.allProperties }

   @Suppress("UNCHECKED_CAST")
   fun load(serialized: PropertyMap) {
      try {
         for ((name, p) in serialized) {
            p as PropertyNode<Any?>
            this[name]?.let { if (p != it) p.load(it.save()) }
         }
      } catch (e: ClassCastException) {
         throw SerializationException("Invalid property data", e)
      }
   }

   fun ClassSerialDescriptorBuilder.buildClassSerialDescriptor() {
      for (p in sortedProperties) {
         element(p.name, p.saveSerializer.descriptor)
      }
   }
   fun CompositeEncoder.encodeProperties(descriptor: SerialDescriptor, offset: Int, value: PropertyMap) {
      for ((i, p) in sortedProperties.withIndex()) {
         val prop = value[p.name] ?: throw SerializationException("Missing sub-property: ${p.name}")
         @Suppress("UNCHECKED_CAST")
         encodeSerializableElement(descriptor, offset + i, p.saveSerializer as SerializationStrategy<Any?>, prop.save())
      }
   }
   fun CompositeDecoder.decodeProperties(
      descriptor: SerialDescriptor, offset: Int, extraHandler: (Int) -> Unit = {}
   ): PropertyMap {
      val props = sortedProperties.toList()
      val values = arrayOfNulls<Any?>(props.size)
      while (true) {
         val i = decodeElementIndex(descriptor)
         if (i == CompositeDecoder.DECODE_DONE) break
         if (i in offset..<offset + props.size) {
            val j = i - offset
            values[j] = decodeSerializableElement(descriptor, i, props[j].saveSerializer, values[j])
         } else extraHandler(i)
      }
      values.indexOfFirst { it == null }.takeIf { it >= 0 }?.let {
         throw SerializationException("Missing sub-property: ${props[it].name}")
      }

      return PropertyMapImpl().apply {
         props.forEachIndexed { i, prop ->
            map[prop.name] = prop
            @Suppress("UNCHECKED_CAST")
            prop as Savable<Any>
            prop.load(values[i]!!)
         }
      }
   }

   fun createSerializer(name: String) = object : KSerializer<PropertyMap> {
      override val descriptor: SerialDescriptor =
         buildClassSerialDescriptor(name) {
            buildClassSerialDescriptor()
         }

      override fun serialize(encoder: Encoder, value: PropertyMap) {
         encoder.encodeStructure(descriptor) {
            encodeProperties(descriptor, 0, value)
         }
      }

      override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
         decodeProperties(descriptor, 0) {
            throw SerializationException("Unexpected serial index: $it")
         }
      }
   }
}

@JvmInline value class PropertyMapImpl(
   val map: MutableMap<String, PropertyNode<*>> = mutableMapOf()
) : PropertyMap, Map<String, PropertyNode<*>> by map

internal fun PropertyHolder.register(property: PropertyNode<*>) {
   val props = properties as PropertyMapImpl
   if (property.name in props)
      throw IllegalStateException("Property ${property.name} already registered")
   props.map[property.name] = property
}

interface PropertyHolder {
   val properties: PropertyMap
   val timeLine: MutableStateValue<TimeLine?>
}

sealed interface PropertyNode<T> : SavableSerializer<T> {
   val holder: PropertyHolder?
   val name: String
   val priority: Int

   fun onChange(block: () -> Unit)
   fun clearChangeListeners()
}

infix fun <N: PropertyNode<*>> N.priority(priority: Int): N = apply {
   setPriority(priority)
}

internal fun PropertyNode<*>.init(name: String, holder: PropertyHolder?) {
   when (this) {
      is AnimProperty<*> -> {
         this.name = name
         this.holder = holder
      }
      is CompoundAnimProperty -> {
         this.name = name
         this.holder = holder
      }
      is PropertyList<*, *> -> {
         this.name = name
         this.holder = holder
      }
   }
}
internal fun PropertyNode<*>.setPriority(priority: Int) {
   when (this) {
      is AnimProperty<*> -> this.priority = priority
      is CompoundAnimProperty -> this.priority = priority
      is PropertyList<*, *> -> this.priority = priority
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

interface PropertyDriver<T> {
   val name: String
   fun interpolate(a: T, b: T, t: Float): T

   companion object {
      @Suppress("UNCHECKED_CAST")
      fun <T> None() = None as PropertyDriver<T>
   }
   object None : PropertyDriver<Any?> {
      override val name = "none"
      override fun interpolate(a: Any?, b: Any?, t: Float): Any? =
         if (t < 1F) a else b
   }
}
