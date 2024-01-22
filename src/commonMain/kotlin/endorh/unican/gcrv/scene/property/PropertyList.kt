package endorh.unican.gcrv.scene.property

import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.util.D
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.div
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KProperty

class PropertyList<S, P: PropertyNode<S>>(
   val factory: () -> P, values: List<P> = emptyList(), val showExpanded: Boolean = true
) : PropertyNode<List<P>>, GeometricProperty2D {
   override var holder: PropertyHolder? = null
      set(value) {
         field = value
         for (p in entries) p.init(name, value)
      }
   override lateinit var name: String
   override var priority: Int = 0
   private val mutableEntries = mutableListOf<P>()
   val entries: List<P> get() = mutableEntries
   init {
      mutableEntries.addAll(values)
   }

   override val geometricCenter: Vec2f get() {
      val center = MutableVec2f()
      var weight = 0F
      for (entry in entries) (entry as? GeometricProperty2D)?.let {
         center.add(entry.geometricCenter)
         weight += entry.geometricWeight
      }
      return center / geometricWeight
   }
   override val geometricWeight get() =
      entries.sumOf { (it as? GeometricProperty2D)?.geometricWeight?.D ?: 0.0 }.F

   override fun applyTransform(transform: Transform2D) {
      for (entry in entries) (entry as? GeometricProperty2D)?.applyTransform(transform)
   }

   private val changeListeners = mutableListOf<() -> Unit>()
   override fun onChange(block: () -> Unit) {
      changeListeners += block
   }
   override fun clearChangeListeners() {
      changeListeners.clear()
   }

   operator fun get(i: Int) = entries[i]

   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   fun insert() = factory().apply {
      init(size.toString(), this@PropertyList.holder)
      onChange { stateChanged() }
   }.also {
      mutableEntries.add(it)
      stateChanged()
   }
   fun remove() {
      if (mutableEntries.isNotEmpty()) {
         mutableEntries.removeLast()
         stateChanged()
      }
   }
   fun clear() {
      if (mutableEntries.isNotEmpty()) {
         mutableEntries.clear()
         stateChanged()
      }
   }

   fun stateChanged() {
      for (listener in changeListeners) listener()
   }

   val size get() = entries.size

   override fun save() = entries
   override fun load(data: List<P>) {
      if (data.size < entries.size) {
         while (data.size != entries.size) remove()
      } else if (data.size > entries.size) {
         while (data.size != entries.size) insert()
      }
      entries.forEachIndexed { i, it ->
         if (it != data[i]) it.load(data[i].save())
      }
   }
   @OptIn(ExperimentalSerializationApi::class)
   override val saveSerializer by lazy { object : KSerializer<List<P>> {
      val elementSerializer = object : KSerializer<P> {
         val serializer = factory().saveSerializer
         override val descriptor = serializer.descriptor
         override fun serialize(encoder: Encoder, value: P) {
            serializer.serialize(encoder, value.save())
         }
         override fun deserialize(decoder: Decoder): P {
            val e = factory()
            e.load(e.saveSerializer.deserialize(decoder))
            return e
         }
      }
      val actualSerializer = ListSerializer(elementSerializer)
      override val descriptor = SerialDescriptor("PropertyList", actualSerializer.descriptor)
      override fun serialize(encoder: Encoder, value: List<P>) =
         encoder.encodeSerializableValue(actualSerializer, value)
      override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(actualSerializer)
   }}
}