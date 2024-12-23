package endorh.unican.gcrv.util

import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiSurface
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun <T> List<T>.toggling(e: T) = if (contains(e)) minus(e) else plus(e)
fun <T> MutableList<T>.toggle(e: T) = if (contains(e)) false.also { remove(e) } else add(e)

fun <T: Any> mutableStateConcurrentList(vararg values: T) = MutableStateConcurrentList(values.toMutableList())

@Serializable(MutableStateConcurrentListSerializer::class)
class MutableStateConcurrentList<T: Any>(
   private val values: MutableList<T> = mutableListOf()
) : MutableState(), MutableList<T> by values {
   var snapshot: List<T> = values.toList()
      private set

   private fun updateState() {
      snapshot = values.toList()
      stateChanged()
   }

   fun use(surface: UiSurface): MutableStateConcurrentList<T> {
      usedBy(surface)
      return this
   }

   override fun add(element: T): Boolean {
      val result = values.add(element)
      if (result) updateState()
      return result
   }

   override fun add(index: Int, element: T) {
      values.add(index, element)
      updateState()
   }

   override fun addAll(index: Int, elements: Collection<T>): Boolean {
      val result = values.addAll(index, elements)
      if (result) updateState()
      return result
   }

   override fun addAll(elements: Collection<T>): Boolean {
      val result = values.addAll(elements)
      if (result) updateState()
      return result
   }

   override fun clear() {
      if (values.isNotEmpty()) {
         values.clear()
         updateState()
      }
   }

   override fun remove(element: T): Boolean {
      val result = values.remove(element)
      if (result) updateState()
      return result
   }

   override fun removeAll(elements: Collection<T>): Boolean {
      val result = values.removeAll(elements)
      if (result) updateState()
      return result
   }

   override fun removeAt(index: Int): T {
      val result = values.removeAt(index)
      updateState()
      return result
   }

   override fun retainAll(elements: Collection<T>): Boolean {
      val result = values.retainAll(elements)
      if (result) updateState()
      return result
   }

   override fun set(index: Int, element: T): T {
      val result = values.set(index, element)
      updateState()
      return result
   }

   override fun toString(): String = values.toString()

   fun atomic(block: MutableStateConcurrentList<T>.() -> Unit) {
      suppressUpdate = true
      block()
      suppressUpdate = false
   }
}

// context(UiSurface)
// fun <T: Any> MutableStateConcurrentList<T>.use() = use(this@UiSurface)

class MutableStateConcurrentListSerializer<T: Any>(private val serializer: KSerializer<T>) : KSerializer<MutableStateConcurrentList<T>> {
   private val listSerializer = ListSerializer(serializer)
   override val descriptor: SerialDescriptor = SerialDescriptor("MutableStateList", listSerializer.descriptor)
   override fun deserialize(decoder: Decoder) =
      MutableStateConcurrentList<T>().apply { addAll(decoder.decodeSerializableValue(listSerializer)) }

   override fun serialize(encoder: Encoder, value: MutableStateConcurrentList<T>) =
      encoder.encodeSerializableValue(listSerializer, value.toList())
}