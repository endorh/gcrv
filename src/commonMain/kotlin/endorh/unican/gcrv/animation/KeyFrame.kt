package endorh.unican.gcrv.animation

import de.fabmax.kool.util.TreeMap
import endorh.unican.gcrv.objects.PropertyInterpolator

data class KeyFrame<T>(
   var time: TimeStamp, var value: T,
   var interpolator: PropertyInterpolator<T> = PropertyInterpolator.None(),
   var easing: Easing = Easing.Linear
)

class KeyFrameList<T> {
   val keyFrames: TreeMap<TimeStamp, KeyFrame<T>> = TreeMap()

   fun set(keyFrame: KeyFrame<T>) {
      keyFrames[keyFrame.time] = keyFrame
   }
   fun set(time: TimeStamp, value: T) {
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
}

sealed interface InterpolationInterval<T> {
   fun interpolate(time: TimeStamp): T

   data class BetweenKeyFrames<T>(val left: KeyFrame<T>, val right: KeyFrame<T>) : InterpolationInterval<T> {
      val duration by lazy { right.time - left.time }
      override fun interpolate(time: TimeStamp) =
         left.interpolator.interpolate(left.value, right.value, left.easing.ease((time - left.time) / duration))
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
