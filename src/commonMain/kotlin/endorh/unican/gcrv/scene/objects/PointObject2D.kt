package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.modules.ui2.remember
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.scene.property.pointStyle
import endorh.unican.gcrv.scene.property.priority
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.util.toVec2i

class PointObject2D(
   pos: Vec2i = Vec2i.ZERO,
   style: PointStyle = PointStyle(Color.WHITE, 7F)
) : Object2D(Type) {
   var pos by geometry(pos)
   val style by pointStyle(style) priority -20

   override val geometricCenter: Vec2f get() = pos
   override val renderers: List<Renderer2D> = listOf(Renderer(this))
   override val gizmos = listOf(gizmo(::pos))
   override val collider: Collider get() = Point2iCollider(pos.toVec2i(), 15)

   class Renderer(val point: PointObject2D) : Renderer2D {
      override fun PointRenderPassInputScope.renderPoints() {
         accept(Point2D(point.pos.toVec2i(), point.style.pointStyle))
      }
   }

   object Type : Object2DType<PointObject2D>("point") {
      private var pointCount = 0
      override fun generateName() = "Point ${++pointCount}"
      override fun create() = PointObject2D()

      override val objectDrawer = object : ObjectDrawer<PointObject2D> {
         val style = mutableStateOf(PointStyle())

         override fun ObjectDrawingContext<PointObject2D>.init() {
            drawnObject.style.pointStyle = style.value
         }
         override fun ObjectDrawingContext<PointObject2D>.onDrag(pos: Vec2f) {
            drawnObject.pos = pos
            update()
         }
         override fun ObjectDrawingContext<PointObject2D>.onDragEnd(pos: Vec2f) {
            finish()
         }

         override fun UiScope.styleEditor(obj: PointObject2D?) {
            PointStyleEditor(style.use(), {
               style.value = it
               obj?.style?.pointStyle = it
            })
         }
      }
   }
}