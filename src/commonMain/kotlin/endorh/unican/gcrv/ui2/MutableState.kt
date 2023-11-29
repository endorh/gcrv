@file:OptIn(ExperimentalSerializationApi::class)

package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.MutableStateList
import de.fabmax.kool.modules.ui2.MutableStateValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias MutableSerialStateValue<T> = @Serializable(MutableStateValueSerializer::class) MutableStateValue<T>
typealias MutableSerialStateList<T> = @Serializable(MutableStateListSerializer::class) MutableStateList<T>

fun <T> mutableSerialStateOf(value: T) = MutableSerialStateValue(value)
fun <T> mutableSerialStateListOf(vararg elements: T) = MutableStateList<T>().apply {
   addAll(elements)
}

class MutableStateValueSerializer<T>(private val serializer: KSerializer<T>) : KSerializer<MutableStateValue<T>> {
   override val descriptor: SerialDescriptor =
      if (serializer.descriptor.kind is PrimitiveKind)
         PrimitiveSerialDescriptor("MutableStateValue", serializer.descriptor.kind as PrimitiveKind)
      else SerialDescriptor("MutableStateValue", serializer.descriptor)
   override fun deserialize(decoder: Decoder) =
      MutableStateValue(decoder.decodeSerializableValue(serializer))
   override fun serialize(encoder: Encoder, value: MutableStateValue<T>) =
      encoder.encodeSerializableValue(serializer, value.value)
}

class MutableStateListSerializer<T>(private val serializer: KSerializer<T>) : KSerializer<MutableStateList<T>> {
   private val listSerializer = ListSerializer(serializer)
   override val descriptor: SerialDescriptor = SerialDescriptor("MutableStateList", listSerializer.descriptor)
   override fun deserialize(decoder: Decoder) =
      MutableStateList<T>().apply { addAll(decoder.decodeSerializableValue(listSerializer)) }
   override fun serialize(encoder: Encoder, value: MutableStateList<T>) =
      encoder.encodeSerializableValue(listSerializer, value.toList())
}