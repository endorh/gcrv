package endorh.unican.gcrv.transformations

import de.fabmax.kool.demo.Settings
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.scene.objects.IFSGizmoObject2D
import endorh.unican.gcrv.scene.objects.IFSGizmoObject2D.ABCSnapshot
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.minus
import endorh.unican.gcrv.util.plus
import endorh.unican.gcrv.util.toVec2i

open class UnionGizmo2D(val gizmos: List<Gizmo2D>, gizmoSize: Float? = null): Gizmo2D {
   constructor(vararg gizmos: Gizmo2D) : this(gizmos.toList())

   private var draggedGizmo: Gizmo2D? = null

   override val collider: Collider2D get() = UnionCollider2D(gizmos.map { it.collider })

   override fun render(canvas: BufferCanvas, transform: Transform2D) {
      gizmos.forEach { it.render(canvas, transform) }
   }

   override fun dragStart(position: Vec2f) {
      gizmos.firstOrNull { position in it.collider }?.let {
         draggedGizmo = it
         it.dragStart(position)
      }
   }

   override fun drag(position: Vec2f) {
      draggedGizmo?.drag(position)
   }

   override fun dragEnd(position: Vec2f) {
      draggedGizmo?.dragEnd(position)
      draggedGizmo = null
   }

   private val _gizmoSize = gizmoSize
   override val gizmoSize: Float get() = _gizmoSize ?: super.gizmoSize
}

fun interface Transform2DGizmoListener {
   fun onTransformUpdate(transform: TaggedTransform2D)
}

open class Transform2DGizmo private constructor(
   taggedTransform: TaggedTransform2D,
   var listener: Transform2DGizmoListener,
   protected val dragGizmo: DragGizmo2D,
   protected val scaleXGizmo: ScaleXGizmo2D,
   protected val scaleYGizmo: ScaleYGizmo2D,
   protected val rotateGizmo: RotateGizmo2D,
   val style: Style = Style(),
): UnionGizmo2D(listOf(dragGizmo, scaleXGizmo, scaleYGizmo, rotateGizmo)) {
   constructor(transform: TaggedTransform2D, listener: Transform2DGizmoListener, style: Style = Style())
     : this(transform, listener, DragGizmo2D(), ScaleXGizmo2D(), ScaleYGizmo2D(), RotateGizmo2D(), style)

   init {
      dragGizmo.o = this
      scaleXGizmo.transformGizmo2D = this
      scaleYGizmo.transformGizmo2D = this
      rotateGizmo.transformGizmo2D = this
   }

   var taggedTransform = taggedTransform
      private set
   val transform = taggedTransform.toTransform()

   var a: Vec2f = transform * Vec2f(-1F, -1F)
   var b: Vec2f = transform * Vec2f(1F, -1F)
   var c: Vec2f = transform * Vec2f(-1F, 1F)

   val d: Vec2f get() = b + c - a
   val center: Vec2f get() = (b + c) / 2F

   fun updateDraft() {
      taggedTransform
   }

   data class Style(
      val centerStyle: PointStyle = PointStyle(Color.LIGHT_GRAY, Settings.gizmoSize.value, CircleAntiAliasPointRenderer),
      val size: Float = Settings.gizmoSize.value,
   )

   class DragGizmo2D : Gizmo2D {
      lateinit var o: Transform2DGizmo
      private lateinit var dragStart: Vec2f
      private lateinit var snapshot: ABCSnapshot
      override val collider get() = Polygon2fCollider(listOf(o.a, o.b, o.d, o.c))

      override fun render(canvas: BufferCanvas, transform: Transform2D) {}
      override fun dragStart(position: Vec2f) {
         dragStart = position
         snapshot = ABCSnapshot(o.a, o.b, o.c)
      }

      override fun drag(position: Vec2f) {
         val offset = position - dragStart
         o.a = snapshot.a + offset
         o.b = snapshot.b + offset
         o.c = snapshot.c + offset
         o.updateDraft()
      }
   }

   abstract class HandleGizmo() : Gizmo2D {
      lateinit var transformGizmo2D: Transform2DGizmo
      abstract val point: Vec2f
      open val renderer: Point2DRenderer get() = CircleAntiAliasPointRenderer
      open val color: Color get() = Color.WHITE
      open val innerColor: Color get() = Color.DARK_GRAY
      open val size get() = gizmoSize
      open val innerSize get() = gizmoSize - 4F

      override val collider: Collider2D get() = Point2fCollider(point)
      override fun render(canvas: BufferCanvas, transform: Transform2D) {
         with(CanvasPixelRendererContext(canvas)) {
            val pos = (transform*point).toVec2i()
            renderPoint(Point2i(pos, PointStyle(color, size, renderer)))
            renderPoint(Point2i(pos, PointStyle(innerColor, innerSize, renderer)))
         }
      }
   }

   protected class PositionGizmo2D: HandleGizmo() {
      override val point: Vec2f
         get() = TODO("Not yet implemented")
      override fun drag(position: Vec2f) {
         TODO("Not yet implemented")
      }
   }

   protected class ScaleXGizmo2D: HandleGizmo() {
      override val point: Vec2f
         get() = TODO("Not yet implemented")
      override fun drag(position: Vec2f) {
         TODO("Not yet implemented")
      }
   }

   protected class ScaleYGizmo2D: HandleGizmo() {
      override val point: Vec2f
         get() = TODO("Not yet implemented")
      override fun drag(position: Vec2f) {
         TODO("Not yet implemented")
      }
   }

   protected class RotateGizmo2D: HandleGizmo() {
      override val point: Vec2f
         get() = TODO("Not yet implemented")
      override fun drag(position: Vec2f) {
         TODO("Not yet implemented")
      }
   }
}