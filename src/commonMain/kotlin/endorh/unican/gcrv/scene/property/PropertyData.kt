package endorh.unican.gcrv.scene.property

import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.serialization.FormatTolerantJsonTransformingSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class AnimPropertyData<T>(val value: T? = null, val keyFrames: KeyFrameList<T>? = null) {
   @OptIn(ExperimentalSerializationApi::class)
   class ConciseJsonSerializer<T>(
      private val serializer: KSerializer<T>,
      availableDrivers: List<PropertyDriver<T>>
   ) : FormatTolerantJsonTransformingSerializer<AnimPropertyData<T>>(Serializer(serializer, availableDrivers)) {
      val shouldUnwrap get() = serializer.descriptor.kind is PrimitiveKind || serializer.descriptor.kind is StructureKind.LIST
      override fun transformSerialize(element: JsonElement) = if (shouldUnwrap) {
         if (element is JsonObject) {
            element["keyFrames"] ?: element["value"]
            ?: throw SerializationException("Unexpected serialized result: $element")
         } else element
      } else element
      override fun transformDeserialize(element: JsonElement) = if (shouldUnwrap) when (element) {
         is JsonObject -> buildJsonObject {
            put("keyFrames", element)
         }
         else -> buildJsonObject {
            put("value", element)
         }
      } else element
   }

   class Serializer<T>(
      private val serializer: KSerializer<T>,
      availableDrivers: List<PropertyDriver<T>>
   ) : KSerializer<AnimPropertyData<T>> {
      val keyFrameListSerializer = KeyFrameList.Serializer(serializer, availableDrivers)
      override val descriptor = buildClassSerialDescriptor("AnimPropertyData") {
         element("value", serializer.descriptor, isOptional = true)
         element("keyFrames", keyFrameListSerializer.descriptor, isOptional = true)
      }
      override fun serialize(encoder: Encoder, value: AnimPropertyData<T>) = encoder.encodeStructure(descriptor) {
         if (value.value != null)
            encodeSerializableElement(descriptor, 0, serializer, value.value)
         if (value.keyFrames != null)
            encodeSerializableElement(descriptor, 1, keyFrameListSerializer, value.keyFrames)
      }
      override fun deserialize(decoder: Decoder): AnimPropertyData<T> = decoder.decodeStructure(descriptor) {
         var value: T? = null
         var keyFrames: KeyFrameList<T>? = null
         while (true) {
            when (val index = decodeElementIndex(descriptor)) {
               0 -> value = decodeSerializableElement(descriptor, 0, serializer, value)
               1 -> keyFrames = decodeSerializableElement(descriptor, 1, keyFrameListSerializer, keyFrames)
               CompositeDecoder.DECODE_DONE -> break
               else -> throw SerializationException("Unexpected serial index: $index")
            }
         }
         AnimPropertyData(value, keyFrames)
      }
   }
}