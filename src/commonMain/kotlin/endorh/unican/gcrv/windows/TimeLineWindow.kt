package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.animation.EasingCurvePopupEditor
import endorh.unican.gcrv.ui2.IntField
import endorh.unican.gcrv.ui2.TimeRangeField
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.ui2.*
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
   private val focusedProperty = mutableSerialStateOf<AnimProperty<*>?>(null)
   private var currentTimeStamp
      get() = timeLine.currentTime.value
      set(value) {
         timeLine.currentTime.value = value
      }

   private val fps = mutableSerialStateOf(30)
   private val snapToGrid = mutableSerialStateOf(true)

   override fun UiScope.windowContent() = Column {
      modifier.size(Grow.Std, Grow.Std)
      Box(Grow.Std) {
         Row {
            Button("+") {
               modifier.margin(4.dp).onClick {
                  focusedProperty.value?.let { p ->
                     p.insertKeyFrame(currentTimeStamp)
                  }
               }
            }

            Button("-") {
               modifier.margin(4.dp).onClick {
                  focusedProperty.value?.let { p ->
                     p.keyFrames.remove(currentTimeStamp)
                  }
               }
            }

            Button("<") {
               modifier.margin(4.dp).onClick {
                  focusedProperty.value?.let { p ->
                     p.keyFrames.lowerKeyFrame(currentTimeStamp)?.time?.let {
                        currentTimeStamp = it
                     }
                  }
               }
            }

            Button(">") {
               modifier.margin(4.dp).onClick {
                  focusedProperty.value?.let { p ->
                     p.keyFrames.higherKeyFrame(currentTimeStamp)?.time?.let {
                        currentTimeStamp = it
                     }
                  }
               }
            }

            focusedProperty.use()?.let { p ->
               @Suppress("UNCHECKED_CAST")
               val keyFrames = p.keyFrames as KeyFrameList<Any>
               keyFrames.ceilingKeyFrame(timeLine.currentTime.value)?.let { kf ->
                  EasingCurvePopupEditor(kf.easing, { keyFrames.set(kf.copy(easing = it)) }) {
                     modifier.margin(4.dp).width(Grow(1F, max=120.dp))
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

      // TimeLineEditor(timeLine, "timeLine") {
      //    modifier.margin(4.dp)
      //       .fpsGrid(fps.use())
      //       .snapToGrid(snapToGrid.use())
      //    selectedProperties.use().firstOrNull()?.let {
      //       modifier.keyFrames(it.keyFrames.allKeyFrames.toList())
      //    } ?: modifier.keyFrames(emptyList())
      // }

      // ScrollArea(withHorizontalScrollbar = false) {
      //    modifier.background(RectBackground(colors.background)).width(Grow.Std).height(Grow.Std)
         TimeLineMultiEditor(timeLine, focusedProperty, "timeLine") {
            modifier.margin(4.dp)
               .background(RectBackground(colors.background))
               .width(Grow.Std)
               .height(Grow.Std)
               .fpsGrid(fps.use())
               .snapToGrid(snapToGrid.use())
            modifier.objects(scene.selectedObjects.use())
         }
      // }
   }
}