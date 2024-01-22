package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.renderers.WireframeRenderPassInputScope
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.scene.property.lineStyle
import endorh.unican.gcrv.scene.property.priority
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.plus

class TriangleObject2D(
   a: Vec2f = Vec2f.ZERO,
   b: Vec2f = Vec2f.X_AXIS,
   c: Vec2f = Vec2f.Y_AXIS,
   style: LineStyle = LineStyle(Color.WHITE),
   pointStyle: PointStyle = PointStyle(Color.WHITE, 5F),
) : Object2D(Type) {
   var a by geometry(a)
   var b by geometry(b)
   var c by geometry(c)

   val style by lineStyle(style, pointStyle, pointStyle) priority -20

   override val geometricCenter: Vec2f get() = (a + b + c) / 3F
   override val renderers: List<Renderer2D> = listOf(Renderer(this))
   override val gizmos = listOf(gizmo(::a), gizmo(::b), gizmo(::c))
   override val collider: Collider2D get() = Polygon2fCollider(listOf(a, b, c))

   override fun toString() = "Triangle[$a, $b, $c]"

   class Renderer(val tri: TriangleObject2D) : Renderer2D {
      override fun WireframeRenderPassInputScope.renderWireframe() = accept(
         LineSegment2f(tri.a, tri.b, tri.style.lineStyle),
         LineSegment2f(tri.b, tri.c, tri.style.lineStyle),
         LineSegment2f(tri.c, tri.a, tri.style.lineStyle))

      override fun PointRenderPassInputScope.renderPoints() = accept(
         Point2f(tri.a, tri.style.start.pointStyle),
         Point2f(tri.b, tri.style.end.pointStyle),
         Point2f(tri.c, tri.style.end.pointStyle))
   }

   object Type : Object2DType<TriangleObject2D>("triangle") {
      private var triangleCount = 0
      override fun generateName() = "Triangle ${++triangleCount}"
      override fun create() = TriangleObject2D()

      override val objectDrawer = object : ObjectDrawer<TriangleObject2D> {
         val style = mutableStateOf(LineStyle())
         var count = 0

         override fun ObjectDrawingContext<TriangleObject2D>.init() {
            count = 0
         }

         override fun ObjectDrawingContext<TriangleObject2D>.onDrag(pos: Vec2f) {
            if (count <= 0) drawnObject.a = pos
            if (count <= 1) drawnObject.b = pos
            drawnObject.c = pos
            update()
         }

         override fun ObjectDrawingContext<TriangleObject2D>.onHover(pos: Vec2f) {
            onDrag(pos)
         }

         override fun ObjectDrawingContext<TriangleObject2D>.onDragEnd(pos: Vec2f) {
            if (count >= 2) finish()
            else count++
         }

         override fun UiScope.styleEditor(obj: TriangleObject2D?) {
            LineStyleEditor(style.use(), {
               style.value = it
               obj?.style?.lineStyle = it
            })
         }
      }
   }
}