package endorh.unican.gcrv.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.KeyFrame
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.animation.TimeRange
import endorh.unican.gcrv.animation.TimeStamp
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface TimeLineScope : UiScope {
   val timeLine: TimeLine
   override val modifier: TimeLineModifier
}

open class TimeLineModifier(surface: UiSurface) : UiModifier(surface) {
   var cursorColor: Color by property(Color.LIGHT_GRAY)
   var gridColor: Color by property(Color.DARK_GRAY)
   var subGridColor: Color by property(Color.DARK_GRAY.withAlpha(0.5F))
   var trackColor: Color by property(Color.GRAY)
   var outOfRangeOverlay: Color by property(Color.DARK_GRAY.withAlpha(0.8F))
   var boundsColor: Color by property(Color.BLUE)

   var keyFrames by property(emptyList<KeyFrame<*>>())
   var fpsGrid: Int? by property(null)
   var snapToGrid: Boolean by property(false)

   var keyFrameDrawable: KeyFrameRenderer = SimpleKeyFrameRenderer(Color.YELLOW)
}

fun <T: TimeLineModifier> T.cursorColor(color: Color) = apply { cursorColor = color }
fun <T: TimeLineModifier> T.gridColor(color: Color) = apply { gridColor = color }
fun <T: TimeLineModifier> T.trackColor(color: Color) = apply { trackColor = color }
fun <T: TimeLineModifier> T.outOfRangeOverlay(color: Color) = apply { outOfRangeOverlay = color }
fun <T: TimeLineModifier> T.boundsColor(color: Color) = apply { boundsColor = color }

fun <T: TimeLineModifier> T.keyFrames(keyFrames: List<KeyFrame<*>>) = apply { this.keyFrames = keyFrames }
fun <T: TimeLineModifier> T.keyFrameDrawable(keyFrameDrawable: KeyFrameRenderer) = apply { this.keyFrameDrawable = keyFrameDrawable }
fun <T: TimeLineModifier> T.fpsGrid(fpsGrid: Int?) = apply { this.fpsGrid = fpsGrid }
fun <T: TimeLineModifier> T.snapToGrid(snapToGrid: Boolean) = apply { this.snapToGrid = snapToGrid }

interface KeyFrameRenderer {
   fun render(node: TimeLineNode, keyFrame: KeyFrame<*>, x: Float, y: Float)
}

class SimpleKeyFrameRenderer(val color: Color) : KeyFrameRenderer {
   override fun render(node: TimeLineNode, keyFrame: KeyFrame<*>, x: Float, y: Float) {
      with(node) {
         node.getUiPrimitives().apply {
            localOval(x, y, 4F, 8F, color)
         }
      }
   }
}

@OptIn(ExperimentalContracts::class)
inline fun UiScope.KeyFrameTimeLine(
   timeLine: TimeLine,
   scopeName: String? = null,
   block: TimeLineScope.() -> Unit = {}
): TimeLineScope {
   contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
   }

   return uiNode.createChild(scopeName, TimeLineNode::class, TimeLineNode.factory).apply {
      init(timeLine)
      modifier.width(Grow.Std)
         .onClick(this)
         .dragListener(this)
         .hoverListener(this)
      block()
   }
}

class TimeLineNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface),
   TimeLineScope, Clickable, Hoverable, Draggable {
   override lateinit var timeLine: TimeLine
   override val modifier = TimeLineModifier(surface)

   fun init(timeLine: TimeLine) {
      if (!::timeLine.isInitialized) {
         this.timeLine = timeLine
         displayedRange.value = timeLine.renderedRange
      }
   }

   val displayedRange = mutableStateOf(TimeRange(TimeStamp(0), TimeStamp(10)))

   val draggedKeyFrame = mutableStateOf<TimeStamp?>(null)

   override fun measureContentSize(ctx: KoolContext) {
      setContentSize(200F, 100F)
   }

   override fun render(ctx: KoolContext) {
      super.render(ctx)

      val time = timeLine.currentTime.use()
      val range = displayedRange.use()
      val renderedRange = timeLine.renderedRange
      val draw = getUiPrimitives()

      modifier.fpsGrid?.let {
         for (t in range.subFrames(it))
            draw.localRect(t.x, 0F, 1F, innerHeightPx, modifier.subGridColor)
      }

      for (t in range.frames)
         draw.localRect(t.x, 0F, 1F, innerHeightPx, modifier.gridColor)


      draw.localRect(0F, 48F, innerWidthPx, 1F, modifier.trackColor)
      for (keyFrame in modifier.keyFrames) {
         val t = keyFrame.time
         if (t in range)
            modifier.keyFrameDrawable.render(this, keyFrame, t.x, 48F)
      }

      if (time in range) {
         draw.localRect(time.x, 0F, 1F, innerHeightPx, modifier.cursorColor)
      }

      if (renderedRange.start in range) {
         draw.localRect(0F, 0F, renderedRange.start.x, innerHeightPx, modifier.outOfRangeOverlay)
      }
      if (renderedRange.end in range) {
         val x = renderedRange.end.x
         draw.localRect(x, 0F, innerWidthPx - x, innerHeightPx, modifier.outOfRangeOverlay)
      }
   }

   private val Float.t: TimeStamp?
      get() = if (this in 0F..1F) {
         val range = displayedRange.value
         range.start + (range.end - range.start) * this
      } else null

   private val TimeStamp.x: Float
      get() {
         val range = displayedRange.value
         return (this - range.start) / (range.end - range.start) * innerWidthPx
      }

   private val TimeStamp.maybeSnap: TimeStamp
      get() = modifier.fpsGrid
         ?.takeIf { modifier.snapToGrid }?.let { round(it) } ?: this

   override fun onClick(ev: PointerEvent) {
      (ev.position.x / innerWidthPx).t?.let {
         timeLine.currentTime.value = it.maybeSnap
      }
   }

   override fun onDrag(ev: PointerEvent) {
      (ev.position.x / innerWidthPx).t?.let {
         timeLine.currentTime.value = it
      }
   }

   override fun onDragEnd(ev: PointerEvent) {
      (ev.position.x / innerWidthPx).t?.let {
         timeLine.currentTime.value = it.maybeSnap
      }
   }

   override fun onDragStart(ev: PointerEvent) {
      super.onDragStart(ev)
   }

   override fun onHover(ev: PointerEvent) {
      super.onHover(ev)
   }

   companion object {
      val factory: (UiNode, UiSurface) -> TimeLineNode = { parent, surface -> TimeLineNode(parent, surface) }
   }
}