@file:OptIn(ExperimentalSerializationApi::class)

package endorh.unican.gcrv.serialization

// I regret everything
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.UI
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

typealias Vec2d = @Serializable(Vec2dSerializer::class) de.fabmax.kool.math.Vec2d
object Vec2dSerializer : KSerializer<Vec2d> {
   val listSerializer = ListSerializer(Double.serializer())
   override val descriptor = SerialDescriptor("Vec2d", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec2d) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y))
   }
   override fun deserialize(decoder: Decoder): Vec2d {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 2) throw SerializationException("Invalid Vec2d: $list (list size is not 2!)")
      return Vec2d(list[0], list[1])
   }
}

typealias Vec2f = @Serializable(Vec2fSerializer::class) de.fabmax.kool.math.Vec2f
object Vec2fSerializer : KSerializer<Vec2f> {
   val listSerializer = ListSerializer(Float.serializer())
   override val descriptor = SerialDescriptor("Vec2f", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec2f) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y))
   }
   override fun deserialize(decoder: Decoder): Vec2f {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 2) throw SerializationException("Invalid Vec2f: $list (list size is not 2!)")
      return Vec2f(list[0], list[1])
   }
}

typealias Vec2i = @Serializable(Vec2iSerializer::class) de.fabmax.kool.math.Vec2i
object Vec2iSerializer : KSerializer<Vec2i> {
   val listSerializer = ListSerializer(Int.serializer())
   override val descriptor = SerialDescriptor("Vec2i", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec2i) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y))
   }
   override fun deserialize(decoder: Decoder): Vec2i {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 2) throw SerializationException("Invalid Vec2i: $list (list size is not 2!)")
      return Vec2i(list[0], list[1])
   }
}

typealias Vec3d = @Serializable(Vec3dSerializer::class) de.fabmax.kool.math.Vec3d
object Vec3dSerializer : KSerializer<Vec3d> {
   val listSerializer = ListSerializer(Double.serializer())
   override val descriptor = SerialDescriptor("Vec3d", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec3d) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y, value.z))
   }
   override fun deserialize(decoder: Decoder): Vec3d {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 3) throw SerializationException("Invalid Vec3d: $list (list size is not 3!)")
      return Vec3d(list[0], list[1], list[2])
   }
}

typealias Vec3f = @Serializable(Vec3fSerializer::class) de.fabmax.kool.math.Vec3f
object Vec3fSerializer : KSerializer<Vec3f> {
   val listSerializer = ListSerializer(Float.serializer())
   override val descriptor = SerialDescriptor("Vec3f", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec3f) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y, value.z))
   }
   override fun deserialize(decoder: Decoder): Vec3f {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 3) throw SerializationException("Invalid Vec3f: $list (list size is not 3!)")
      return Vec3f(list[0], list[1], list[2])
   }
}

typealias Vec3i = @Serializable(Vec3iSerializer::class) de.fabmax.kool.math.Vec3i
object Vec3iSerializer : KSerializer<Vec3i> {
   val listSerializer = ListSerializer(Int.serializer())
   override val descriptor = SerialDescriptor("Vec3i", listSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: Vec3i) {
      encoder.encodeSerializableValue(listSerializer, listOf(value.x, value.y, value.z))
   }
   override fun deserialize(decoder: Decoder): Vec3i {
      val list = decoder.decodeSerializableValue(listSerializer)
      if (list.size != 3) throw SerializationException("Invalid Vec3i: $list (list size is not 3!)")
      return Vec3i(list[0], list[1], list[2])
   }
}

fun de.fabmax.kool.util.Color.Companion.randomHue() =
   de.fabmax.kool.util.Color.Hsv(Random.nextFloat() * 360F, 1F, 1F).toSrgb()
typealias Color = @Serializable(ColorSerializer::class) de.fabmax.kool.util.Color
object ColorSerializer : KSerializer<Color> {
   override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)
   override fun serialize(encoder: Encoder, value: Color) =
      encoder.encodeSerializableValue(String.serializer(), value.toHexString(true).uppercase())
   override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(String.serializer()).let {
      Color.fromHexOrNull(it) ?: throw SerializationException("Invalid color: \"$it\"")
   }
}
object ColorUIntARGBSerializer : KSerializer<Color> {
   override val descriptor = SerialDescriptor("Color", UInt.serializer().descriptor)
   override fun serialize(encoder: Encoder, value: Color) {
      encoder.encodeSerializableValue(
         UInt.serializer(),
         (value.a * 0xFF).UI shl 24 or
           ((value.r * 0xFF).UI shl 16) or
           (value.g * 0xFF).UI shl 8 or
           (value.b * 0xFF).UI
      )
   }
   private val div = 0xFF.F
   override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(UInt.serializer()).let {
      Color(
         ((it shr 16) and 0xFFu).F / div,
         ((it shr 8) and 0xFFu).F / div,
         (it and 0xFFu).F / div,
         (it shr 24).F / div
      )
   }
}


typealias TreeMap<K, V> = @Serializable(TreeMapSerializer::class) de.fabmax.kool.util.TreeMap<K, V>
class TreeMapSerializer<K: Any, V: Any>(keySerializer: KSerializer<K>, valSerializer: KSerializer<V>) : KSerializer<TreeMap<K, V>> {
   val mapSerializer = MapSerializer(keySerializer, valSerializer)
   override val descriptor = SerialDescriptor("TreeMap", mapSerializer.descriptor)
   override fun serialize(encoder: Encoder, value: TreeMap<K, V>) =
      encoder.encodeSerializableValue(mapSerializer, value)
   override fun deserialize(decoder: Decoder) = TreeMap<K, V>().apply {
      putAll(decoder.decodeSerializableValue<Map<K, V>>(mapSerializer))
   }
}
