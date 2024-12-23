package endorh.unican.gcrv.windows.editor

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.animation.EasingCurvePopupEditor
import endorh.unican.gcrv.animation.TimeStamp
import endorh.unican.gcrv.ui2.IntField
import endorh.unican.gcrv.ui2.TimeRangeField
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import endorh.unican.gcrv.util.pad
import endorh.unican.gcrv.util.padLength
import endorh.unican.gcrv.windows.BaseWindow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class TimeLineWindow(scene: EditorScene) : BaseWindow<EditorScene>("Timeline", scene, true) {

   init {
      windowDockable.setFloatingBounds(width = Dp(450F), height = Dp(150F))
   }

   private val timeLine get() = scene.timeLine.value
   private val playback get() = scene.playbackManager.value
   private val selectedProperties get() = scene.selectedProperties
   private val focusedProperty = mutableSerialStateOf<AnimProperty<*>?>(null)
   private val focusedProperties = mutableSerialStateListOf<AnimProperty<*>>()
   private var currentTimeStamp
      get() = timeLine.currentTime.value
      set(value) {
         timeLine.currentTime.value = value
      }

   private val fps get() = scene.fps
   private val snapToGrid = mutableSerialStateOf(true)

   override fun UiScope.windowContent() = Column {
      modifier.size(Grow.Std, Grow.Std)
      Box(Grow.Std) {
         Row {
            Button("+") {
               modifier.margin(4.dp).onClick {
                  focusedProperties.forEach {
                     it.insertKeyFrame(currentTimeStamp)
                  }
               }
            }

            Button("-") {
               modifier.margin(4.dp).onClick {
                  focusedProperties.forEach { it ->
                     it.keyFrames.remove(currentTimeStamp)
                  }
               }
            }

            Button("|<") {
               modifier.margin(4.dp).onClick {
                  focusedProperties.mapNotNull { it.keyFrames.lowerKeyFrame(currentTimeStamp)?.time }
                     .maxOrNull()?.let {
                        currentTimeStamp = it
                     }
               }
            }

            Button("<") {
               modifier.margin(4.dp).onClick {
                  val sub = floor(currentTimeStamp.subFrame * fps.value) / fps.value
                  if (sub != currentTimeStamp.subFrame) {
                     currentTimeStamp = currentTimeStamp.copy(subFrame = sub)
                  } else {
                     val lower = (round(sub * fps.value) - 1F) / fps.value
                     if (lower < 0) {
                        if (currentTimeStamp.frame > 0)
                           currentTimeStamp = TimeStamp(currentTimeStamp.frame - 1, (fps.value - 1F) / fps.value.F)
                     } else currentTimeStamp = currentTimeStamp.copy(subFrame = lower)
                  }
               }
            }

            Button(">") {
               modifier.margin(4.dp).onClick {
                  val sub = ceil(currentTimeStamp.subFrame * fps.value) / fps.value
                  if (sub != currentTimeStamp.subFrame) {
                     currentTimeStamp = currentTimeStamp.copy(subFrame = sub)
                  } else {
                     val higher = (round(sub * fps.value) + 1F) / fps.value
                     if (higher > 1F) {
                        if (currentTimeStamp.frame < timeLine.renderedRange.end.frame)
                           currentTimeStamp = TimeStamp(currentTimeStamp.frame + 1, 0F)
                     } else currentTimeStamp = currentTimeStamp.copy(subFrame = higher)
                  }
               }
            }

            Button(">|") {
               modifier.margin(4.dp).onClick {
                  focusedProperties.mapNotNull { it.keyFrames.higherKeyFrame(currentTimeStamp)?.time }
                     .minOrNull()?.let {
                        currentTimeStamp = it
                     }
               }
            }

            focusedProperties.use().takeIf { it.isNotEmpty() }?.let { props ->
               @Suppress("UNCHECKED_CAST")
               val keyFrames = props.first().keyFrames as KeyFrameList<Any>
               keyFrames.ceilingKeyFrame(timeLine.currentTime.use())?.let { kf ->
                  EasingCurvePopupEditor(kf.easing, {
                     val time = timeLine.currentTime.value
                     keyFrames.ceilingKeyFrame(time)?.let { kf ->
                        keyFrames.set(kf.copy(easing = it.copy()))
                     }
                     // focusedProperties.forEach { p ->
                     //    @Suppress("UNCHECKED_CAST")
                     //    val pKeyFrames = p.keyFrames as KeyFrameList<Any>
                     //    pKeyFrames.ceilingKeyFrame(time)?.let { kf ->
                     //       pKeyFrames.set(kf.copy(easing = it.copy()))
                     //    }
                     // }
                  }, hideOnClickOutside = false) {
                     modifier.margin(4.dp).width(Grow(1F, max = 120.dp))
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
            Button(if (playback.isPlaying) "| |" else "|>", "play/pause") {
               modifier.margin(4.dp).onClick {
                  playback.togglePause()
               }
            }
            Button("[ ]", "stop") {
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

      // ScrollArea(withHorizontalScrollbar = false) {
      //    modifier.background(RectBackground(colors.background)).width(Grow.Std).height(Grow.Std)
         TimeLineMultiEditor(timeLine, focusedProperties, "timeLine") {
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