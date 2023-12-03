package endorh.unican.gcrv.animation

import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiSurface
import endorh.unican.gcrv.scene.property.PropertyDriver
import endorh.unican.gcrv.serialization.TreeMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

data class KeyFrame<T>(
   var time: TimeStamp, var value: T,
   var driver: PropertyDriver<T> = PropertyDriver.None(),
   var easing: Easing = Easing.CubicBezier.Linear()
) {
   class Serializer<T>(
      val serializer: KSerializer<T>,
      val availableDrivers: List<PropertyDriver<T>>,
      val ignoreTime: Boolean
   ) : KSerializer<KeyFrame<T>> {
      override val descriptor: SerialDescriptor = buildClassSerialDescriptor("KeyFrame") {
         element("time", TimeStamp.serializer().descriptor, isOptional = ignoreTime)
         element("value", serializer.descriptor)
         element("interpolator", String.serializer().descriptor, isOptional = true)
         element("easing", Easing.serializer().descriptor, isOptional = true)
      }
      override fun serialize(encoder: Encoder, value: KeyFrame<T>) = encoder.encodeStructure(descriptor) {
         if (!ignoreTime)
            encodeSerializableElement(descriptor, 0, TimeStamp.serializer(), value.time)
         encodeSerializableElement(descriptor, 1, serializer, value.value)
         if (availableDrivers.size > 1 && value.driver != availableDrivers[0])
            encodeSerializableElement(descriptor, 2, String.serializer(), value.driver.name)
         if (value.easing != Easing.Linear && (value.easing as? Easing.CubicBezier)?.isLinear != true)
            encodeSerializableElement(descriptor, 3, Easing.serializer(), value.easing)
      }
      override fun deserialize(decoder: Decoder): KeyFrame<T> = decoder.decodeStructure(descriptor) {
         var time: TimeStamp? = null
         var value: T? = null
         var valueWritten = false
         var driverName: String? = null
         var easing: Easing? = null
         while (true) {
            when (val index = decodeElementIndex(descriptor)) {
               0 -> time = decodeSerializableElement(descriptor, 0, TimeStamp.serializer(), time)
               1 -> value = decodeSerializableElement(descriptor, 1, serializer, value).also { valueWritten = true }
               2 -> driverName = decodeSerializableElement(descriptor, 2, String.serializer(), driverName)
               3 -> easing = decodeSerializableElement(descriptor, 3, Easing.serializer(), easing)
               CompositeDecoder.DECODE_DONE -> break
               else -> error("Unexpected serial index: $index")
            }
         }
         if (time == null && !ignoreTime) throw SerializationException("Missing time!")
         if (!valueWritten) throw SerializationException("Missing value!")
         @Suppress("UNCHECKED_CAST")
         value as T
         val driver = driverName?.let { name ->
            availableDrivers.firstOrNull { it.name == name }
               ?: throw SerializationException("Unknown driver: $name")
         } ?: availableDrivers.firstOrNull() ?: PropertyDriver.None()
         KeyFrame(time ?: TimeStamp(0), value, driver, easing ?: Easing.CubicBezier.Linear())
      }
   }
}

class KeyFrameList<T> : MutableState() {
   private val keyFrames: TreeMap<TimeStamp, KeyFrame<T>> = TreeMap()

   operator fun contains(time: TimeStamp) = time in keyFrames
   operator fun get(time: TimeStamp) = keyFrames[time]
   fun set(keyFrame: KeyFrame<T>) {
      keyFrames[keyFrame.time] = keyFrame
   }
   operator fun set(time: TimeStamp, value: T) {
      keyFrames[time] = KeyFrame(time, value)
   }
   fun setAll(keyFrames: Iterable<KeyFrame<T>>) {
      for (keyFrame in keyFrames)
         set(keyFrame)
   }
   fun remove(keyFrame: KeyFrame<T>) {
      if (keyFrames[keyFrame.time] == keyFrame) {
         keyFrames.remove(keyFrame.time)
      }
   }
   fun remove(time: TimeStamp) {
      keyFrames.remove(time)
   }

   fun clear() {
      keyFrames.clear()
   }

   val allKeyFrames: Sequence<KeyFrame<T>> get() = keyFrames.values.asSequence()

   fun floorKeyFrame(time: TimeStamp) = keyFrames.floorValue(time)
   fun ceilingKeyFrame(time: TimeStamp) = keyFrames.ceilingValue(time)
   fun lowerKeyFrame(time: TimeStamp) = keyFrames.lowerValue(time)
   fun higherKeyFrame(time: TimeStamp) = keyFrames.higherValue(time)

   fun intervalForTime(time: TimeStamp): InterpolationInterval<T>? {
      val left = floorKeyFrame(time)
      if (left == null) {
         val right = ceilingKeyFrame(time) ?: return null
         return InterpolationInterval.BeforeFirstKeyFrame(right)
      } else if (left.time == time) {
         return InterpolationInterval.ExactKeyFrame(left)
      } else {
         val right = ceilingKeyFrame(time)
            ?: return InterpolationInterval.AfterLastKeyFrame(left)
         return InterpolationInterval.BetweenKeyFrames(left, right)
      }
   }
   fun valueForTime(time: TimeStamp) = intervalForTime(time)?.interpolate(time)

   fun copy() = KeyFrameList<T>().also {
      it.keyFrames.putAll(keyFrames)
   }

   val size get() = keyFrames.size
   fun isEmpty() = keyFrames.isEmpty()
   fun isNotEmpty() = keyFrames.isNotEmpty()

   fun use(surface: UiSurface) = apply {
      usedBy(surface)
   }
   fun clearUse(surface: UiSurface) {
      clearUsage(surface)
   }

   @OptIn(ExperimentalSerializationApi::class)
   class Serializer<T>(serializer: KSerializer<T>, availableDrivers: List<PropertyDriver<T>>) : KSerializer<KeyFrameList<T>> {
      val mapSerializer = MapSerializer(TimeStamp.serializer(), KeyFrame.Serializer(serializer, availableDrivers, true))
      override val descriptor: SerialDescriptor = SerialDescriptor("KeyFrameList", mapSerializer.descriptor)
      override fun serialize(encoder: Encoder, value: KeyFrameList<T>) =
         encoder.encodeSerializableValue(mapSerializer, value.keyFrames)
      override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(mapSerializer).let { map ->
         KeyFrameList<T>().also {
            it.setAll(map.map { (t, k) -> k.copy(time = t) })
         }
      }
   }
}

sealed interface InterpolationInterval<T> {
   fun interpolate(time: TimeStamp): T

   data class BetweenKeyFrames<T>(val left: KeyFrame<T>, val right: KeyFrame<T>) : InterpolationInterval<T> {
      val duration by lazy { right.time - left.time }
      override fun interpolate(time: TimeStamp) =
         left.driver.interpolate(left.value, right.value, right.easing.ease((time - left.time) / duration))
   }

   data class BeforeFirstKeyFrame<T>(val keyFrame: KeyFrame<T>) : InterpolationInterval<T> {
      override fun interpolate(time: TimeStamp) = keyFrame.value
   }

   data class AfterLastKeyFrame<T>(val keyFrame: KeyFrame<T>) : InterpolationInterval<T> {
      override fun interpolate(time: TimeStamp) = keyFrame.value
   }

   data class ExactKeyFrame<T>(val keyFrame: KeyFrame<T>) : InterpolationInterval<T> {
      override fun interpolate(time: TimeStamp) = keyFrame.value
   }
}
