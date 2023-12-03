package endorh.unican.gcrv.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.geometry.TextProps
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Font
import endorh.unican.gcrv.animation.KeyFrame
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.animation.TimeRange
import endorh.unican.gcrv.animation.TimeStamp
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.scene.property.CompoundAnimProperty
import endorh.unican.gcrv.scene.property.PropertyList
import endorh.unican.gcrv.scene.property.PropertyNode
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.toTitleCase
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface TimeLineMultiEditorScope : UiScope {
   val timeLine: TimeLine
   override val modifier: TimeLineMultiEditorModifier
}

open class TimeLineMultiEditorModifier(surface: UiSurface) : UiModifier(surface) {
   var cursorColor: Color by property(Color.LIGHT_GRAY)
   var gridColor: Color by property(Color.DARK_GRAY.mix(Color.GRAY, 0.5F))
   var subGridColor: Color by property(Color.DARK_GRAY.withAlpha(0.8F))
   var trackSize: Dp by property(Dp(12F))
   var trackColor: Color by property(Color.GRAY)
   var focusedTrackColor: Color by property(Color.DARK_MAGENTA)
   var outOfRangeOverlay: Color by property(Color.BLACK.withAlpha(0.4F))
   var boundsColor: Color by property(Color.DARK_BLUE)

   var propertyFont: Font by property(Font.DEFAULT_FONT.derive(14F))
   var propertyTextColor: Color by property(Color.WHITE)
   var focusedPropertyTextColor: Color by property(Color.MAGENTA)
   var propertyWidth: Dp by property(Dp(80F))

   var objects: List<Object2D> by property(emptyList())
      internal set
   internal var properties: List<List<PropertyNode<*>>> = emptyList()

   var fpsGrid: Int? by property(null)
   var snapToGrid: Boolean by property(false)

   var keyFrameDrawable: KeyFrameMultiRenderer = SimpleKeyFrameMultiRenderer(Color.YELLOW)
}

fun <T: TimeLineMultiEditorModifier> T.cursorColor(color: Color) = apply { cursorColor = color }
fun <T: TimeLineMultiEditorModifier> T.gridColor(color: Color) = apply { gridColor = color }
fun <T: TimeLineMultiEditorModifier> T.subGridColor(color: Color) = apply { subGridColor = color }
fun <T: TimeLineMultiEditorModifier> T.trackSize(size: Dp) = apply { trackSize = size }
fun <T: TimeLineMultiEditorModifier> T.trackColor(color: Color) = apply { trackColor = color }
fun <T: TimeLineMultiEditorModifier> T.outOfRangeOverlay(color: Color) = apply { outOfRangeOverlay = color }
fun <T: TimeLineMultiEditorModifier> T.boundsColor(color: Color) = apply { boundsColor = color }
fun <T: TimeLineMultiEditorModifier> T.propertyFont(font: Font) = apply { propertyFont = font }
fun <T: TimeLineMultiEditorModifier> T.propertyTextColor(color: Color) = apply { propertyTextColor = color }
fun <T: TimeLineMultiEditorModifier> T.focusedPropertyTextColor(color: Color) = apply { focusedPropertyTextColor = color }
fun <T: TimeLineMultiEditorModifier> T.propertyWidth(width: Dp) = apply { propertyWidth = width }

fun <T: TimeLineMultiEditorModifier> T.objects(objects: List<Object2D>) = apply {
   if (this.objects != objects) {
      properties = objects
         .flatMap { it.properties.values }
         .groupBy { it.name }
         .values.sortedBy { it.first() is CompoundAnimProperty }
      this.objects = objects
   }
}
fun <T: TimeLineMultiEditorModifier> T.keyFrameDrawable(keyFrameDrawable: KeyFrameMultiRenderer) = apply { this.keyFrameDrawable = keyFrameDrawable }
fun <T: TimeLineMultiEditorModifier> T.fpsGrid(fpsGrid: Int?) = apply { this.fpsGrid = fpsGrid }
fun <T: TimeLineMultiEditorModifier> T.snapToGrid(snapToGrid: Boolean) = apply { this.snapToGrid = snapToGrid }

interface KeyFrameMultiRenderer {
   fun render(node: TimeLineMultiEditorNode, keyFrame: KeyFrame<*>, x: Float, y: Float)
}

class SimpleKeyFrameMultiRenderer(val color: Color) : KeyFrameMultiRenderer {
   override fun render(node: TimeLineMultiEditorNode, keyFrame: KeyFrame<*>, x: Float, y: Float) {
      with(node) {
         node.getUiPrimitives().apply {
            localOval(x, y, 4F, 8F, color)
         }
      }
   }
}

@OptIn(ExperimentalContracts::class)
inline fun UiScope.TimeLineMultiEditor(
   timeLine: TimeLine,
   focusedProperty: MutableStateValue<AnimProperty<*>?>,
   scopeName: String? = null,
   block: TimeLineMultiEditorScope.() -> Unit = {}
): TimeLineMultiEditorScope {
   contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
   }

   return uiNode.createChild(scopeName, TimeLineMultiEditorNode::class, TimeLineMultiEditorNode.factory).apply {
      init(timeLine, focusedProperty)
      modifier.width(Grow.Std)
         .onClick(this)
         .dragListener(this)
         .hoverListener(this)
         .onWheelY {
            scrollState.scrollDpY(it.pointer.deltaScrollY.F * -50F, false)
            // println("Scrolled: ${scrollState.computeSmoothScrollPosDpY()}, ${it.pointer.deltaScrollY.F}, [${scrollState.contentHeightDp.value}]")
         }
      block()
   }
}

class TimeLineMultiEditorNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface),
   TimeLineMultiEditorScope, Clickable, Hoverable, Draggable {
   override lateinit var timeLine: TimeLine
   override val modifier = TimeLineMultiEditorModifier(surface)
   private var focusedProperty: MutableStateValue<AnimProperty<*>?> = mutableSerialStateOf(null)

   private val textCache = mutableMapOf<String, CachedTextGeometry>()

   fun init(timeLine: TimeLine, focusedProperty: MutableStateValue<AnimProperty<*>?>? = null) {
      if (!::timeLine.isInitialized) {
         this.timeLine = timeLine
         displayedRange.value = timeLine.renderedRange
      }
      focusedProperty?.let {
         this.focusedProperty = it
      }
   }

   val displayedRange = mutableSerialStateOf(TimeRange(TimeStamp(0), TimeStamp(10)))
   val scrollState = rememberScrollState()

   val draggedKeyFrame = mutableSerialStateOf<TimeStamp?>(null)

   override fun measureContentSize(ctx: KoolContext) {
      val th = modifier.trackSize.px
      var h = 24F + 12F
      fun visit(p: PropertyNode<*>, suppressTitle: Boolean = false) {
         h += th
         if (p is CompoundAnimProperty) {
            if (suppressTitle) h -= th
            for (sub in p.properties.values)
               visit(sub)
         } else if (p is PropertyList<*, *>) {
            for (sub in p.entries) {
               visit(sub, true)
               if (sub is CompoundAnimProperty) h += 4F
            }
         }
      }
      for (p in modifier.properties) p.firstOrNull()?.let { visit(it) }
      setContentSize(200F, h)
      scrollState.contentHeightDp.value = Dp.fromPx(maxOf(0F, h - heightPx)).value
   }

   fun drawPropName(name: String, x: Float, y: Float, color: Color): CachedTextGeometry {
      val cache = textCache.getOrPut(name) { CachedTextGeometry(this) }
      val metrics = cache.textMetrics
      val props = TextProps(modifier.propertyFont).apply {
         font = modifier.propertyFont
         text = name
         isYAxisUp = false
         origin.set(x, y + 4F - metrics.height / 2F + metrics.yBaseline, 0F)
      }
      cache.addTextGeometry(
         getTextBuilder(modifier.propertyFont).geometry,
         props, color, 0F, Vec4f(clipBoundsPx.x, clipBoundsPx.y, clipBoundsPx.x + timeLineStartPx, clipBoundsPx.w))
      return cache
   }

   private var timeLineStartPx = 0F
   private var timeLineWidthPx = 0F

   override fun render(ctx: KoolContext) {
      super.render(ctx)

      val time = timeLine.currentTime.use()
      val range = displayedRange.use()
      val yScroll = scrollState.yScrollDp.use().dp.px
      val renderedRange = timeLine.renderedRange
      val draw = getUiPrimitives()

      timeLineStartPx = modifier.propertyWidth.px
      timeLineWidthPx = innerWidthPx - timeLineStartPx

      // Draw subgrid
      modifier.fpsGrid?.let {
         for (t in range.subFrames(it))
            draw.localRect(t.x, 0F, 1F, innerHeightPx, modifier.subGridColor)
      }

      // Draw grid
      for (t in range.frames) {
         draw.localRect(t.x, 0F, 1F, innerHeightPx, modifier.gridColor)
      }

      // Draw tracks
      val trackH = modifier.trackSize.px
      var trackX = paddingStartPx
      var trackY = 24F + 12F - yScroll
      val treeLineColor = modifier.propertyTextColor.withAlpha(0.5F)
      fun drawTrack(node: PropertyNode<*>, suppressTitle: Boolean = false) {
         val focused = node == focusedProperty.value
         if (node !is CompoundAnimProperty || !suppressTitle) drawPropName(
            node.name.toTitleCase(), trackX, trackY,
            if (focused) modifier.focusedPropertyTextColor else modifier.propertyTextColor)
         when (node) {
            is CompoundAnimProperty -> {
               if (!suppressTitle) {
                  trackY += trackH
                  trackX += 8F
               }
               val startY = trackY
               for (sub in node.properties.values) {
                  if (trackY > innerHeightPx) return
                  drawTrack(sub)
               }
               if (!suppressTitle) {
                  draw.localRect(
                     trackX - 4F, startY - trackH / 2 + 2F, 1F, trackY - startY - 4F,
                     treeLineColor)
                  trackX -= 8F
               }
            }
            is PropertyList<*, *> -> {
               trackY += trackH
               trackX += 8F
               for (sub in node.entries) {
                  if (trackY > innerHeightPx) return
                  val startY = trackY
                  drawTrack(sub, true)
                  draw.localRect(
                     trackX - 4F, startY - trackH / 2 + 2F, 1F, trackY - startY - 4F,
                     treeLineColor)
                  if (sub is CompoundAnimProperty) trackY += 4F
               }
               trackX -= 8F
            }
            is AnimProperty<*> -> {
               if (trackY >= 0F) {
                  draw.localRect(
                     timeLineStartPx, trackY, timeLineWidthPx, 1F,
                     if (focused) modifier.focusedTrackColor else modifier.trackColor
                  )
                  for (keyFrame in node.keyFrames.allKeyFrames) {
                     if (keyFrame.time in range)
                        modifier.keyFrameDrawable.render(this, keyFrame, keyFrame.time.x, trackY)
                  }
               }
               trackY += trackH
            }
         }
      }
      for (propSet in modifier.properties) {
         propSet.firstOrNull()?.let { drawTrack(it) }
         if (trackY > innerHeightPx) break
      }

      // Draw cursor
      if (time in range) {
         draw.localRect(time.x, 0F, 1F, timeLineWidthPx, modifier.cursorColor)
      }

      if (renderedRange.start in range) {
         val x = renderedRange.start.x
         if (x > timeLineStartPx + 2F) {
            draw.localRect(timeLineStartPx, 0F, x - timeLineStartPx, innerHeightPx, modifier.outOfRangeOverlay)
            draw.localRect(x - 2, 0F, 2F, innerHeightPx, modifier.boundsColor)
         }
      }
      if (renderedRange.end in range) {
         val x = renderedRange.end.x
         draw.localRect(x, 0F, innerWidthPx - x, innerHeightPx, modifier.outOfRangeOverlay)
         draw.localRect(x, 0F, 2F, innerHeightPx, modifier.boundsColor)
      }
   }

   private val PointerEvent.timeLineX: Float
      get() = (position.x - timeLineStartPx) / timeLineWidthPx

   private val PointerEvent.track: AnimProperty<*>? get() {
      val y = (position.y + scrollState.yScrollDp.value.dp.px)
      val trackH = modifier.trackSize.px
      var py = 24F + 12F + trackH/2
      fun visit(p: PropertyNode<*>, suppressTitle: Boolean = false): AnimProperty<*>? {
         if (py >= y && p is AnimProperty<*>) return p
         py += trackH
         if (p is CompoundAnimProperty) {
            if (suppressTitle) py -= trackH
            for (sub in p.properties.values)
               visit(sub)?.let { return it }
         } else if (p is PropertyList<*, *>) {
            for (sub in p.entries) {
               visit(sub, true)?.let { return it }
               if (sub is CompoundAnimProperty) py += 4F
            }
         }
         return null
      }
      for (p in modifier.properties)
         p.firstOrNull()?.let { node -> visit(node)?.let { return it } }
      return null
   }

   private val Float.unitClamp get() = if (this < 0F) 0F else if (this > 1F) 1F else this

   private val Float.t: TimeStamp?
      get() = if (this in 0F..1F) {
         val range = displayedRange.value
         range.start + (range.end - range.start) * this
      } else null

   private val TimeStamp.tx: Float get() {
      val range = displayedRange.value
      return (this - range.start) / (range.end - range.start) * timeLineWidthPx
   }

   private val TimeStamp.x: Float get() {
      val range = displayedRange.value
      return (this - range.start) / (range.end - range.start) * timeLineWidthPx + timeLineStartPx
   }

   private val TimeStamp.maybeSnap: TimeStamp
      get() = modifier.fpsGrid
         ?.takeIf { modifier.snapToGrid }?.let { round(it) } ?: this

   override fun onClick(ev: PointerEvent) {
      ev.timeLineX.t?.let {
         timeLine.currentTime.value = it.maybeSnap
      }
      ev.track?.let {
         focusedProperty.value = it
      }
   }

   override fun onDrag(ev: PointerEvent) {
      ev.timeLineX.unitClamp.t?.let {
         timeLine.currentTime.value = it
      }
   }

   override fun onDragEnd(ev: PointerEvent) {
      ev.timeLineX.unitClamp.t?.let {
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
      val factory: (UiNode, UiSurface) -> TimeLineMultiEditorNode = { parent, surface -> TimeLineMultiEditorNode(parent, surface) }
   }
}