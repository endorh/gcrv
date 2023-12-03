package endorh.unican.gcrv.serialization

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.writeJson


/**
 * Copy of [JsonTransformingSerializer] that supports non-JSON formats.
 *
 * When (de)serializing to/from non-JSON formats it will just delegate to the [delegate] serializer.
 *
 * There are two methods in which JSON transformation can be defined: [transformSerialize] and [transformDeserialize].
 * You can override one or both of them. Consult their documentation for details.
 *
 * Usage example:
 *
 * ```
 * @Serializable
 * data class Example(
 *     @Serializable(UnwrappingJsonListSerializer::class) val data: String
 * )
 * // Unwraps a list to a single object
 * object UnwrappingJsonListSerializer :
 *     FormatTolerantJsonTransformingSerializer<String>(String.serializer()) {
 *     override fun transformDeserialize(element: JsonElement): JsonElement {
 *         if (element !is JsonArray) return element
 *         require(element.size == 1) { "Array size must be equal to 1 to unwrap it" }
 *         return element.first()
 *     }
 * }
 * // Now these functions both yield correct result:
 * Json.parse(Example.serializer(), """{"data":["str1"]}""")
 * Json.parse(Example.serializer(), """{"data":"str1"}""")
 * ```
 *
 * @param T A type for Kotlin property for which this serializer could be applied.
 *        **Not** the type that you may encounter in JSON. (e.g. if you unwrap a list
 *        to a single value `T`, use `T`, not `List<T>`)
 * @param delegate A serializer for type [T]. Determines [JsonElement] which is passed to [transformSerialize].
 *        Should be able to parse [JsonElement] from [transformDeserialize] function.
 *        Usually, default [serializer] is sufficient.
 */
@OptIn(InternalSerializationApi::class)
abstract class FormatTolerantJsonTransformingSerializer<T : Any>(
   private val delegate: KSerializer<T>
) : KSerializer<T> {

   /**
    * A descriptor for this transformation.
    * By default, it delegates to [delegate]'s descriptor.
    *
    * However, this descriptor can be overridden to achieve better representation of the resulting JSON shape
    * for schema generating or introspection purposes.
    */
   override val descriptor: SerialDescriptor get() = delegate.descriptor

   final override fun serialize(encoder: Encoder, value: T) {
      (encoder as? JsonEncoder)?.let { output ->
         var element = output.json.writeJson(value, delegate)
         element = transformSerialize(element)
         output.encodeJsonElement(element)
      } ?: delegate.serialize(encoder, value)
   }

   final override fun deserialize(decoder: Decoder): T {
      return (decoder as? JsonDecoder)?.let { input ->
         val element = input.decodeJsonElement()
         input.json.decodeFromJsonElement(delegate, transformDeserialize(element))
      } ?: delegate.deserialize(decoder)
   }

   /**
    * Transformation that happens during [deserialize] call.
    * Does nothing by default.
    *
    * During deserialization, a value from JSON is firstly decoded to a [JsonElement],
    * user transformation in [transformDeserialize] is applied,
    * and then resulting [JsonElement] is deserialized to [T] with [delegate].
    */
   protected open fun transformDeserialize(element: JsonElement): JsonElement = element

   /**
    * Transformation that happens during [serialize] call.
    * Does nothing by default.
    *
    * During serialization, a value of type [T] is serialized with [delegate] to a [JsonElement],
    * user transformation in [transformSerialize] is applied, and then resulting [JsonElement] is encoded to a JSON string.
    */
   protected open fun transformSerialize(element: JsonElement): JsonElement = element
}
