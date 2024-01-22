package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.fractals.IFSFunctionDraft
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.renderers.PolyFillRenderPassInputScope
import endorh.unican.gcrv.renderers.WireframeRenderPassInputScope
import endorh.unican.gcrv.renderers.line.BresenhamRendererBreadthAntiAlias
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.renderers.point.SquarePointRenderer
import endorh.unican.gcrv.renderers.point.TiltedSquarePointRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.transformations.TaggedTransform2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.*
import kotlin.properties.Delegates

class IFSGizmoObject2D(
   draft: IFSFunctionDraft = IFSFunctionDraft(),
) : Object2D(Type) {
   var draft = draft
      set(value) {
         field.removeChangeCallback(::updatePoints)
         field = value
         value.onChange(::updatePoints)
      }
   init {
      this.draft = draft // Trigger setter
   }
   val lineStyle = LineStyle(renderer=BresenhamRendererBreadthAntiAlias)
   val pointStyle = PointStyle()

   var a: Vec2f by geometry(draft.transform.value.toTransform() * Vec2f(-1F, -1F))
   var b: Vec2f by geometry(draft.transform.value.toTransform() * Vec2f(1F, -1F))
   var c: Vec2f by geometry(draft.transform.value.toTransform() * Vec2f(-1F, 1F))

   val d: Vec2f get() = b + c - a
   val center: Vec2f get() = (b + c) / 2F

   protected var suppressUpdate: Boolean = false
   fun updatePoints() {
      if (!suppressUpdate) {
         a = draft.transform.value.toTransform() * Vec2f(-1F, -1F)
         b = draft.transform.value.toTransform() * Vec2f(1F, -1F)
         c = draft.transform.value.toTransform() * Vec2f(-1F, 1F)
      }
   }
   fun updateDraft() {
      suppressUpdate = true
      draft.transform.value = TaggedTransform2D.fromThreePoints(a, b, c)
      suppressUpdate = false
   }

   override val geometricCenter: Vec2f get() = center
   override val renderers: List<Renderer2D> = listOf(Renderer(this))

   data class ABCSnapshot(val a: Vec2f, val b: Vec2f, val c: Vec2f)
   class DragGizmo(val o: IFSGizmoObject2D) : Gizmo2D {
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

   abstract class HandleGizmo(val o: IFSGizmoObject2D) : Gizmo2D {
      abstract val point: Vec2f
      open val renderer: Point2DRenderer get() = CircleAntiAliasPointRenderer
      open val color: Color get() = Color.WHITE
      open val innerColor: Color get() = Color.DARK_GRAY
      open val size get() = gizmoSize
      open val innerSize get() = gizmoSize - 4F

      override val collider: Collider2D get() = Point2fCollider(point)
      override fun render(canvas: BufferCanvas, transform: Transform2D) {
         with (CanvasPixelRendererContext(canvas)) {
            val pos = (transform * point).toVec2i()
            renderPoint(Point2i(pos, PointStyle(color, size, renderer)))
            renderPoint(Point2i(pos, PointStyle(innerColor, innerSize, renderer)))
         }
      }
   }

   class RotateGizmo(o: IFSGizmoObject2D) : HandleGizmo(o) {
      override val point get() = o.d
      private var angleStart by Delegates.notNull<Float>()
      private lateinit var centerStart: Vec2f
      private lateinit var snapshot: ABCSnapshot

      override fun dragStart(position: Vec2f) {
         centerStart = o.center
         angleStart = Vec2f.X_AXIS.signedAngle(position - centerStart)
         snapshot = ABCSnapshot(o.a - centerStart, o.b - centerStart, o.c - centerStart)
      }
      override fun drag(position: Vec2f) {
         val angle = Vec2f.X_AXIS.signedAngle(position - centerStart) - angleStart
         o.a = centerStart + snapshot.a.rotateRad(angle)
         o.b = centerStart + snapshot.b.rotateRad(angle)
         o.c = centerStart + snapshot.c.rotateRad(angle)
         o.updateDraft()
      }
   }

   class ScaleXGizmo(o: IFSGizmoObject2D) : HandleGizmo(o) {
      override val point get() = o.b
      override val renderer get() = SquarePointRenderer
      private lateinit var ref: Vec2f
      private lateinit var direction: Vec2f

      override fun dragStart(position: Vec2f) {
         direction = o.b - o.a
         if (direction.sqrLength() <= 1e-8F)
            direction = Vec2f.X_AXIS
         ref = o.a
      }
      override fun drag(position: Vec2f) {
         o.b = ref + direction * (((position - ref) * direction) / direction.sqrLength())
         o.updateDraft()
      }
   }

   class ScaleYGizmo(o: IFSGizmoObject2D) : HandleGizmo(o) {
      override val point get() = o.c
      override val renderer get() = SquarePointRenderer
      private lateinit var ref: Vec2f
      private lateinit var direction: Vec2f

      override fun dragStart(position: Vec2f) {
         direction = o.c - o.a
         if (direction.sqrLength() <= 1e-8F)
            direction = Vec2f.X_AXIS
         ref = o.a
      }

      override fun drag(position: Vec2f) {
         o.c = ref + direction * (((position - ref) * direction) / direction.sqrLength())
         o.updateDraft()
      }
   }

   class ShearGizmo(o: IFSGizmoObject2D) : HandleGizmo(o) {
      override val point get() = o.a
      override val renderer get() = TiltedSquarePointRenderer

      override fun drag(position: Vec2f) {
         o.a = position
         o.updateDraft()
      }
   }

   override val gizmos = listOf(
      DragGizmo(this),
      ScaleXGizmo(this),
      ScaleYGizmo(this),
      RotateGizmo(this),
      ShearGizmo(this))
   override val collider: Collider2D get() = Polygon2fCollider(listOf(a, b, c))

   override fun toString() = "IFS[${draft.transform}]"

   class Renderer(val g: IFSGizmoObject2D) : Renderer2D {
      override fun PolyFillRenderPassInputScope.renderPolyFills() = accept(
         PolyFill2f(listOf(g.a, g.b, g.d, g.c), PolyFillStyle(g.draft.color.value)))

      override fun WireframeRenderPassInputScope.renderWireframe() = accept(
         LineSegment2f(g.a, g.b, g.lineStyle),
         LineSegment2f(g.a, g.c, g.lineStyle),
         LineSegment2f(g.b, g.d, g.lineStyle),
         LineSegment2f(g.c, g.d, g.lineStyle))

      override fun PointRenderPassInputScope.renderPoints() = accept(
         Point2f(g.a, g.pointStyle),
         Point2f(g.b, g.pointStyle),
         Point2f(g.c, g.pointStyle),
         Point2f(g.d, g.pointStyle))
   }

   object Type : Object2DType<IFSGizmoObject2D>("ifs-gizmo") {
      private var ifsGizmoCount = 0
      override fun generateName() = "IFS Gizmo ${++ifsGizmoCount}"
      override fun create() = IFSGizmoObject2D()
   }
}
