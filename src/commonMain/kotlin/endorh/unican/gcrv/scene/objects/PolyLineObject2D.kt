package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.renderers.WireframeRenderPassInputScope
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.scene.property.Vec2fProperty
import endorh.unican.gcrv.scene.property.lineStyle
import endorh.unican.gcrv.scene.property.priority

class PolyLineObject2D(
   points: List<Vec2f> = emptyList(),
   style: LineStyle = LineStyle(Color.WHITE),
   pointStyle: PointStyle = PointStyle(Color.WHITE, 5F),
) : Object2D(Type) {
   val points by geoList(*points.toTypedArray())
   val style by lineStyle(style, pointStyle, pointStyle) priority -20

   init {
      this.points.onChange {
         _gizmos = null
      }
   }

   override val geometricCenter: Vec2f get() = points.geometricCenter
   override val renderers: List<Renderer2D> = listOf(Renderer(this))
   private var _gizmos: List<Gizmo2D>? = null
   override val gizmos: List<Gizmo2D> get() {
      _gizmos?.let { return it }
      return points.entries.map { gizmo(it) }.also {
         _gizmos = it
      }
   }
   override val collider: Collider2D get() = UnionCollider2D(
      points.entries.windowed(2) {
         val (a, b) = it
         LineSegment2fCollider(LineSegment2f(a.value, b.value))
      })

   override fun toString() = "PolyLine[${points.entries.joinToString { it.value.toString() }}]"

   class Renderer(val poly: PolyLineObject2D) : Renderer2D {
      override fun WireframeRenderPassInputScope.renderWireframe() {
         for (i in 0 until poly.points.entries.size - 1)
            accept(LineSegment2f(
               poly.points[i].value, poly.points[i + 1].value,
               poly.style.lineStyle))
      }

      override fun PointRenderPassInputScope.renderPoints() = accept(
         poly.points.entries.map { Point2f(it.value, poly.style.start.pointStyle) })
   }

   object Type : Object2DType<PolyLineObject2D>("polyline") {
      private var polyLineCount = 0
      override fun generateName() = "Poly-Line ${++polyLineCount}"
      override fun create() = PolyLineObject2D()

      override val objectDrawer = object : ObjectDrawer<PolyLineObject2D> {
         val style = mutableStateOf(LineStyle())
         var count = 0
         lateinit var point: Vec2fProperty

         override fun ObjectDrawingContext<PolyLineObject2D>.init() {
            count = 0
            point = drawnObject.points.insert()
         }

         override fun ObjectDrawingContext<PolyLineObject2D>.onDrag(pos: Vec2f) {
            point.value = pos
            update()
         }

         override fun ObjectDrawingContext<PolyLineObject2D>.onHover(pos: Vec2f) {
            onDrag(pos)
         }

         override fun ObjectDrawingContext<PolyLineObject2D>.onDragEnd(pos: Vec2f) {
            point.value = pos
            point = drawnObject.points.insert()
            point.value = pos
            count++
         }

         override fun ObjectDrawingContext<PolyLineObject2D>.onRightClick() {
            if (count >= 3) {
               drawnObject.points.remove()
               finish()
            }
         }

         override fun UiScope.styleEditor(obj: PolyLineObject2D?) {
            LineStyleEditor(style.use(), {
               style.value = it
               obj?.style?.lineStyle = it
            })
         }
      }
   }
}