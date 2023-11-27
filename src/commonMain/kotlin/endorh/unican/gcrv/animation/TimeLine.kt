package endorh.unican.gcrv.animation

import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.RenderLoop
import endorh.unican.gcrv.objects.Object2DStack
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class PlaybackManager(context: CoroutineContext, val timeLine: TimeLine) : MutableState(), CoroutineScope {
   override val coroutineContext: CoroutineContext = context + Dispatchers.RenderLoop
   var isPlaying = false
      private set(value) {
         field = value
         stateChanged()
      }
   var isLoop = true
      private set(value) {
         field = value
         stateChanged()
      }
   private var playPosition: TimeStamp? = null
   private var playJob: Job? = null

   var step = TimeStamp(0, 1F / 30F)
      set(value) {
         field = value
         stateChanged()
      }
   var fps = 30F
      set(value) {
         field = value
         frameDelay = (1000F / fps).roundToLong()
         stateChanged()
      }
   private var frameDelay = (1000F / fps).roundToLong()

   fun play() {
      if (isPlaying) return
      playPosition = timeLine.currentTime.value
      isPlaying = true
      playJob = launch {
         delay(frameDelay)
         while (isPlaying) {
            if (isActive) advance(step)
            delay(frameDelay)
         }
      }
   }

   fun pause() {
      isPlaying = false
      playJob?.cancel()
   }

   fun stop() {
      isPlaying = false
      playJob?.cancel()
      playPosition?.let {
         timeLine.currentTime.value = it
         playPosition = null
      }
   }

   fun togglePause() {
      if (isPlaying) pause() else play()
   }

   fun toggleStop() {
      if (isPlaying) stop() else play()
   }

   fun advance(amount: TimeStamp) {
      timeLine.currentTime.value += amount
      if (timeLine.currentTime.value > timeLine.renderedRange.end) {
         if (isLoop) {
            timeLine.currentTime.value -= (timeLine.renderedRange.end - timeLine.renderedRange.start)
         } else {
            timeLine.currentTime.value = timeLine.renderedRange.end
            isPlaying = false
         }
      }
   }

   fun use(surface: UiSurface) {
      usedBy(surface)
   }
   fun UiScope.use() = use(surface)
}

class TimeLine {
   var renderedRange: TimeRange = TimeRange(TimeStamp(0), TimeStamp(10))
   var currentTime = mutableStateOf(TimeStamp(0))

   var animatedObjects: Object2DStack = Object2DStack()
}

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
}

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
}

