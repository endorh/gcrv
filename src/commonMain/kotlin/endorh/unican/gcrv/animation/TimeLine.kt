package endorh.unican.gcrv.animation

import de.fabmax.kool.modules.ui2.mutableStateOf
import endorh.unican.gcrv.ui2.MutableSerialStateValue
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import endorh.unican.gcrv.util.roundToString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Serializable
class TimeLine {
   var renderedRange: TimeRange = TimeRange(TimeStamp(0), TimeStamp(10))
   var currentTime: MutableSerialStateValue<TimeStamp> = mutableStateOf(TimeStamp(0))
}

@Serializable(TimeStamp.Serializer::class)
data class TimeStamp(val frame: Int, val subFrame: Float = 0F) : Comparable<TimeStamp> {
   operator fun rangeTo(other: TimeStamp) = TimeRange(this, other)
   operator fun rangeUntil(other: TimeStamp) = TimeRange(this, other, true)

   override fun compareTo(other: TimeStamp) = if (frame == other.frame)
      subFrame.compareTo(other.subFrame)
   else frame.compareTo(other.frame)

   operator fun plus(other: TimeStamp) = (subFrame + other.subFrame).let {
      if (it > 1) TimeStamp(frame + other.frame + 1, it - 1)
      else TimeStamp(frame + other.frame, it)
   }

   operator fun minus(other: TimeStamp) = (subFrame - other.subFrame).let {
      if (it < 0) TimeStamp(frame - other.frame - 1, it + 1)
      else TimeStamp(frame - other.frame, it)
   }

   operator fun div(other: TimeStamp) = (frame + subFrame) / (other.frame + other.subFrame)
   operator fun times(t: Float) = ((frame + subFrame) * t).let {
      val f = floor(it)
      TimeStamp(f.I, it - f)
   }

   fun round(fps: Int) = ((subFrame * fps).roundToInt().F / fps).let {
      if (it >= 1F) TimeStamp(frame + 1, it - 1)
      else TimeStamp(frame, it)
   }

   override fun toString() = "[$frame:$subFrame]"
   fun asSeconds(): Float = frame + subFrame

   companion object {
      fun fromSeconds(seconds: Float) = TimeStamp(seconds.I, seconds - seconds.I)
   }

   object Serializer : KSerializer<TimeStamp> {
      override val descriptor: SerialDescriptor
         get() = PrimitiveSerialDescriptor("TimeStamp", PrimitiveKind.STRING)

      override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())
      // TODO: Use a better way to represent frames exactly
      override fun serialize(encoder: Encoder, value: TimeStamp) {
         encoder.encodeString(toString(value))
      }

      fun fromString(serialized: String): TimeStamp {
         if (':' !in serialized) throw SerializationException("Invalid TimeStamp format: $serialized (missing colon)")
         val split = serialized.split(":")
         if (split.size != 2) throw SerializationException("Invalid TimeStamp format: $serialized (too many colons, expected 1)")
         val frame = split[0].toIntOrNull() ?: throw SerializationException("Invalid TimeStamp format: $serialized (invalid frame: '${split[0]}')")
         var subFrameText = split[1]
         if ('.' !in subFrameText)
            subFrameText = "0.$subFrameText"
         val subFrame = subFrameText.toFloatOrNull() ?: throw SerializationException("Invalid TimeStamp format: $serialized (invalid sub-frame: '${split[1]}')")
         return TimeStamp(frame, subFrame)
      }
      fun toString(value: TimeStamp) = "${value.frame}:${value.subFrame.roundToString(5).removePrefix("0.")}"
   }
}

@Serializable(TimeRange.Serializer::class)
data class TimeRange(val start: TimeStamp, val end: TimeStamp, val exclusiveEnd: Boolean = false) {
   operator fun contains(timeStamp: TimeStamp) =
      timeStamp >= start && if (exclusiveEnd) timeStamp < end else timeStamp <= end

   val frames: Sequence<TimeStamp> = sequence {
      var t = if (start.subFrame == 0F) start else TimeStamp(start.frame + 1)
      while (t < end) {
         yield(t)
         t = TimeStamp(t.frame + 1)
      }
   }

   fun subFrames(fps: Int) = sequence {
      var i = start.frame
      var j = floor(start.subFrame * fps).I
      val lj = ceil(end.subFrame * fps).I
      val fFps = fps.F
      while (i < end.frame || i == end.frame && j < lj) {
         yield(TimeStamp(i, j / fFps))
         j += 1
         if (j > fps) {
            j %= fps
            i += 1
         }
      }
   }

   object Serializer : KSerializer<TimeRange> {
      override val descriptor = PrimitiveSerialDescriptor("TimeRange", PrimitiveKind.STRING)
      override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())
      override fun serialize(encoder: Encoder, value: TimeRange) {
         encoder.encodeString(toString(value))
      }

      fun fromString(serialized: String): TimeRange {
         if (serialized.startsWith('[')) {
            val exclusiveEnd = serialized.endsWith(')')
            if (!exclusiveEnd && !serialized.endsWith(']'))
               throw SerializationException("Invalid TimeRange format: $serialized (doesn't end with ']' or ')')")
            if ('-' !in serialized) throw SerializationException("Invalid TimeRange format: $serialized (missing '-')")
            val (start, end) = serialized.substring(1, serialized.length - 1).split("-")
            return TimeRange(TimeStamp.Serializer.fromString(start), TimeStamp.Serializer.fromString(end), exclusiveEnd)
         } else throw SerializationException("Invalid TimeRange format: $serialized (doesn't start with '[')")
      }
      fun toString(value: TimeRange) = "[${TimeStamp.Serializer.toString(value.start)}-${TimeStamp.Serializer.toString(value.end)}${if (value.exclusiveEnd) ")" else "]"}"
   }
}

