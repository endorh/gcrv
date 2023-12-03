package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.scene.property.lineStyle
import endorh.unican.gcrv.scene.property.priority
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.renderers.WireframeRenderPassInputScope
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.plus
import endorh.unican.gcrv.util.toVec2i

class LineObject2D(
   start: Vec2i = Vec2i.ZERO,
   end: Vec2i = Vec2i.X_AXIS,
   style: LineStyle = LineStyle(Color.WHITE),
   startStyle: PointStyle = PointStyle(Color.WHITE, 5F),
   endStyle: PointStyle = PointStyle(Color.WHITE, 5F),
) : Object2D(Type) {
   var start by geometry(start)
   var end by geometry(end)
   val style by lineStyle(style, startStyle, endStyle) priority -20

   override val geometricCenter: Vec2f get() = (start + end) / 2F
   override val renderers: List<Renderer2D> = listOf(Renderer(this))
   override val gizmos = listOf(gizmo(::start), gizmo(::end))
   override val collider: Collider get() = Line2iCollider(Line2D(start.toVec2i(), end.toVec2i()), 15)

   class Renderer(val line: LineObject2D) : Renderer2D {
      override fun WireframeRenderPassInputScope.renderWireframe() {
         accept(Line2D(line.start.toVec2i(), line.end.toVec2i(), line.style.lineStyle))
      }
      override fun PointRenderPassInputScope.renderPoints() {
         accept(
            Point2D(line.start.toVec2i(), line.style.start.pointStyle),
            Point2D(line.end.toVec2i(), line.style.end.pointStyle)
         )
      }
   }

   object Type : Object2DType<LineObject2D>("line") {
      private var lineCount = 0
      override fun generateName() = "Line ${++lineCount}"
      override fun create() = LineObject2D()

      override val objectDrawer = object : ObjectDrawer<LineObject2D> {
         val style = mutableStateOf(LineStyle())
         var count = 0
         var startPos = Vec2f.ZERO
         override fun ObjectDrawingContext<LineObject2D>.init() {
            count = 0
         }

         override fun ObjectDrawingContext<LineObject2D>.onDragStart(pos: Vec2f) {
            if (count == 0) {
               startPos = pos
               drawnObject.start = pos
            }
         }

         override fun ObjectDrawingContext<LineObject2D>.onDrag(pos: Vec2f) {
            if (count == 0 && pos != startPos) count++
            drawnObject.end = pos
            update()
         }
         override fun ObjectDrawingContext<LineObject2D>.onHover(pos: Vec2f) {
            onDrag(pos)
         }
         override fun ObjectDrawingContext<LineObject2D>.onDragEnd(pos: Vec2f) {
            if (count > 0) finish()
            else count++
         }

         override fun UiScope.styleEditor(obj: LineObject2D?) {
            LineStyleEditor(style.use(), {
               style.value = it
               obj?.style?.lineStyle = it
            })
         }
      }
   }
}