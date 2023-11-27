package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.line_algorithms.ui.EasingEditor
import endorh.unican.gcrv.line_algorithms.ui.IntField
import endorh.unican.gcrv.line_algorithms.ui.TimeRangeField
import endorh.unican.gcrv.ui2.KeyFrameTimeLine
import endorh.unican.gcrv.ui2.fpsGrid
import endorh.unican.gcrv.ui2.keyFrames
import endorh.unican.gcrv.ui2.snapToGrid
import endorh.unican.gcrv.util.I
import endorh.unican.gcrv.util.pad
import endorh.unican.gcrv.util.padLength

class TimeLineWindow(scene: EditorScene) : BaseWindow("Timeline", scene, true) {

   init {
      windowDockable.setFloatingBounds(width = Dp(450F), height = Dp(150F))
   }

   private val timeLine get() = scene.timeLine.value
   private val playback get() = scene.playbackManager.value
   private val selectedProperties get() = scene.selectedProperties
   private var currentTimeStamp
      get() = timeLine.currentTime.value
      set(value) {
         timeLine.currentTime.value = value
      }

   private val fps = mutableStateOf(30)
   private val snapToGrid = mutableStateOf(true)

   override fun UiScope.windowContent() = Column {
      modifier.size(Grow.Std, Grow.Std)
      Box(Grow.Std) {
         Row {
            Button("+") {
               modifier.margin(4.dp).onClick {
                  for (p in selectedProperties.value) {
                     p.insertKeyFrame(currentTimeStamp)
                  }
               }
            }

            Button("-") {
               modifier.margin(4.dp).onClick {
                  for (p in selectedProperties.value)
                     p.keyFrames.remove(currentTimeStamp)
               }
            }

            Button("<") {
               modifier.margin(4.dp).onClick {
                  selectedProperties.value.firstOrNull()?.let {
                     it.keyFrames.keyFrames.lowerKey(currentTimeStamp)?.let {
                        currentTimeStamp = it
                     }
                  }
               }
            }

            Button(">") {
               modifier.margin(4.dp).onClick {
                  selectedProperties.value.firstOrNull()?.let {
                     it.keyFrames.keyFrames.higherKey(currentTimeStamp)?.let {
                        currentTimeStamp = it
                     }
                  }
               }
            }

            selectedProperties.use().firstOrNull()?.let { p ->
               @Suppress("UNCHECKED_CAST")
               val keyFrames = p.keyFrames as KeyFrameList<Any>
               keyFrames.keyFrames.floorValue(timeLine.currentTime.value)?.let { kf ->
                  EasingEditor(kf.easing, { keyFrames.set(kf.copy(easing = it)) }, "easing") {
                     modifier.margin(4.dp)
                  }
               }
            }
         }
         Row(Grow(1F, max=FitContent), scopeName = "playback") {
            modifier.alignX(AlignmentX.End)
            playback.use(surface)

            val t = timeLine.currentTime.use()
            Text("${t.frame}:${(t.subFrame * fps.use()).I.pad(fps.value.padLength, "0")}") {
               modifier.alignY(AlignmentY.Center).width(50.dp).textAlignX(AlignmentX.End)
            }
            Button("<<", "reset") {
               modifier.margin(4.dp).onClick {
                  timeLine.currentTime.value = timeLine.renderedRange.start
               }
            }
            Button(if (playback.isPlaying) "||" else "|>", "play/pause") {
               modifier.margin(4.dp).onClick {
                  playback.togglePause()
               }
            }
            Button("[]", "stop") {
               modifier.margin(4.dp).onClick {
                  playback.stop()
               }
            }

            TimeRangeField(timeLine.renderedRange, { timeLine.renderedRange = it }) {
               modifier.margin(4.dp).width(150.dp)
            }

            Text("FPS Grid") {
               modifier.alignY(AlignmentY.Center)
            }
            IntField(fps.use(), { fps.value = it }, "fps") {
               modifier.margin(4.dp).width(50.dp)
            }
            Checkbox(snapToGrid.use()) {
               modifier.margin(4.dp).onToggle { snapToGrid.value = it }.alignY(AlignmentY.Center)
            }
         }
      }

      KeyFrameTimeLine(timeLine, "timeLine") {
         modifier.margin(4.dp)
            .fpsGrid(fps.use())
            .snapToGrid(snapToGrid.use())
         selectedProperties.use().firstOrNull()?.let {
            modifier.keyFrames(it.keyFrames.allKeyFrames.toList())
         } ?: modifier.keyFrames(emptyList())
      }
   }
}