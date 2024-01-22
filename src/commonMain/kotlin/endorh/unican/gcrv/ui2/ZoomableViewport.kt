package endorh.unican.gcrv.ui2

import de.fabmax.kool.math.MutableVec2d
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.clamp
import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.Object2DStack
import endorh.unican.gcrv.scene.TransformedCollider2D
import endorh.unican.gcrv.scene.TransformedGizmo
import endorh.unican.gcrv.serialization.Vec2i
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.util.*
import kotlin.math.pow
import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty

open class ZoomableViewport(
   baseRect: Rect2d = Rect2d(-1.0, -1.0, 1.0, 1.0),
   var clamp: Boolean = true
) : MutableState() {
   protected fun <T> state(value: T) = Delegates.observed(value) { invalidate() }
   protected fun <T> state(value: T, extraAction: (T) -> Unit) = Delegates.observed(value) {
      invalidate()
      extraAction(it)
   }
   protected fun <T> computedState(computation: () -> T): ReadOnlyProperty<Any, T> = Delegates.lazySnapshot(::snapshot, computation)

   private val changeListeners = mutableListOf<(ZoomableViewport) -> Unit>()
   var rect by state(baseRect)
   var viewportRect = Rect2d(0.0, 0.0, 1.0, 1.0)
      private set
   var targetDim by state(Vec2f(1F, 1F)) {
      viewportRect = Rect2d(0.0, 0.0, it.x.D, it.y.D)
   }

   var zoomLevel by state(1.0)
   var zoomCenter by state(baseRect.center)
   var zoomAnchor by state(baseRect.center)

   var dragPanning = false

   fun resetZoom() {
      zoomLevel = 1.0
      zoomCenter = rect.center
      zoomAnchor = rect.center
   }

   protected var snapshot: Int = 0
      private set
   protected var lastDisplayedRect: Rect2d? = null
   protected fun invalidate() {
      snapshot++
      stateChanged()
      if (lastDisplayedRect != displayedRect) {
         lastDisplayedRect = displayedRect
         changeListeners.forEach { it(this) }
      }
   }

   val displayedRect: Rect2d by computedState {
      val w: Double
      val h: Double
      val zoom = zoomLevel
      val center = zoomCenter
      val rectAR = rect.height / rect.width
      val imageAR = targetDim.y.D / targetDim.x.D
      if (imageAR > rectAR) {
         w = rect.width / zoom
         h = rect.height / zoom * imageAR / rectAR
      } else {
         w = rect.width / zoom * rectAR / imageAR
         h = rect.height / zoom
      }

      val sx = center.x - w / 2
      val sy = center.y - h / 2
      Rect2d(sx, sy, sx + w, sy + h)
   }
   fun canvasTransform(canvasSize: Vec2i, invertY: Boolean = false): Transform2D {
      val r = displayedRect
      val scale = Vec2f(canvasSize.x.F / r.width.F, canvasSize.y.F / r.height.F)
      with (Transform2D) {
         val t = translate(
            Vec2f(-scale.x * r.center.x.F, -scale.y * r.center.y.F) + canvasSize.toVec2f() / 2F
         ) * scale(scale)
         return if (invertY) {
            scale(1F, -1F) * translate(0F, -canvasSize.y.F) * t
         } else t
      }
   }
   val uvRect by computedState {
      val rect = displayedRect
      UvRect(
         Vec2f(rect.left.F, rect.top.F), Vec2f(rect.right.F, rect.top.F),
         Vec2f(rect.left.F, rect.bottom.F), Vec2f(rect.right.F, rect.bottom.F))
   }

   fun zoomBy(delta: Double) {
      val nextZoom = (zoomLevel * 2.0.pow(delta)).let { if (clamp) it.clamp(1.0, 1e8) else it }
      val factor = zoomLevel / nextZoom
      val anchor = zoomAnchor
      val displayedRect = displayedRect
      val center = MutableVec2d(displayedRect.scaleFrom(anchor, factor).center)
      val w = displayedRect.width * factor
      val h = displayedRect.height * factor
      if (clamp) {
         if (center.x + w / 2 > rect.right) center.x = rect.right - w / 2
         if (center.x - w / 2 < rect.left) center.x = rect.left + w / 2
         if (center.y + h / 2 > rect.bottom) center.y = rect.bottom - h / 2
         if (center.y - h / 2 < rect.top) center.y = rect.top + h / 2
      }
      zoomCenter = Vec2d(center)
      zoomLevel = nextZoom
   }

   fun setAnchorFromViewport(viewportAnchor: Vec2d) {
      zoomAnchor = displayedRect.globalize(viewportRect.relativize(viewportAnchor))
   }

   fun onChange(block: (ZoomableViewport) -> Unit) = apply {
      changeListeners += block
   }
   fun removeOnChange(block: (ZoomableViewport) -> Unit) {
      changeListeners -= block
   }
}

inline fun UiModifier.viewportControls(
   viewport: ZoomableViewport, noinline panEventFilter: (PointerEvent) -> Boolean = { true }
) = onMeasured {
   viewport.targetDim = Vec2f(maxOf(1F, it.widthPx), maxOf(1F, it.heightPx))
}.onDragStart {
   if (panEventFilter(it)) viewport.dragPanning = true
}.onDrag {
   if (viewport.dragPanning) {
      val x = it.pointer.deltaX
      val y = it.pointer.deltaY
      val displayed = viewport.displayedRect
      val dx = x / viewport.targetDim.x * displayed.width
      val dy = y / viewport.targetDim.y * displayed.height
      viewport.zoomCenter = Vec2d(viewport.zoomCenter.x - dx, viewport.zoomCenter.y + dy)
   }
}.onDragEnd {
   if (panEventFilter(it)) viewport.dragPanning = false
}.onWheelY {
   val pos = it.position
   viewport.setAnchorFromViewport(Vec2d(pos.x.D, viewport.targetDim.y.D - pos.y.D))
   viewport.zoomBy(it.pointer.deltaScrollY)
}

inline fun CanvasScope.pickingControls(
   objectStack: Object2DStack, filteredObjects: Collection<Object2D>? = null,
   viewport: ZoomableViewport? = null,
   noinline eventFilter: (PointerEvent) -> Boolean = { it.pointer.isLeftButtonClicked },
   noinline onPicked: (Object2D?) -> Unit
) {
   modifier.onClick { ev ->
      if (eventFilter(ev)) {
         val transform = viewport?.canvasTransform(canvasSize, invertY = true)
         val pos = ev.canvasPosition
         val colliders = objectStack.collectColliders(filteredObjects, false, transform)
         colliders.asSequence()
            .filter { pos in it.first }
            .sortedWith(
               compareBy<Pair<TransformedCollider2D, Object2D>> { it.first.dimension }
                  .thenBy { it.first.area }
                  .thenBy { it.first.centerDistance(pos) }
            ).firstOrNull()?.let {
               onPicked(it.second)
            } ?: onPicked(null)
      }
   }
}

inline fun CanvasScope.gizmoControls(
   objectStack: Object2DStack, selectedObjects: Collection<Object2D>? = null,
   viewport: ZoomableViewport? = null, noinline onDraggingListener: (Boolean) -> Unit = {},
   noinline eventFilter: (PointerEvent) -> Boolean = { it.pointer.isLeftButtonDown }
) {
   val gizmo: MutableStateValue<TransformedGizmo?> = remember { mutableStateOf(null) }
   modifier.onDragStart { ev ->
      if (eventFilter(ev)) {
         // Start dragging a gizmo
         val transform = viewport?.canvasTransform(canvasSize, invertY = true)
         val pos = ev.canvasPosition
         val gizmos = objectStack.collectGizmos(selectedObjects, false, transform)
         gizmos.asSequence()
            .filter { pos in it.collider }
            .sortedWith(
               compareBy<TransformedGizmo> { it.collider.dimension }
                  .thenBy { it.collider.area }
                  .thenBy { it.collider.centerDistance(pos) }
            ).firstOrNull()?.let {
               it.dragStart(pos)
               gizmo.value = it
            }
         onDraggingListener(true)
      }
   }.onDrag { ev ->
      gizmo.value?.drag(ev.canvasPosition)
   }.onDragEnd { ev ->
      gizmo.value?.let {
         it.dragEnd(ev.canvasPosition)
         onDraggingListener(false)
      }
      gizmo.value = null
   }
}