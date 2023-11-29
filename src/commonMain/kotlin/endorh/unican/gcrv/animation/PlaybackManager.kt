package endorh.unican.gcrv.animation

import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.util.RenderLoop
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
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