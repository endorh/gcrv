package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.CubicSplineRenderPassInputScope
import endorh.unican.gcrv.renderers.PointRenderPassInputScope
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.renderers.point.SquarePointRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.scene.ControlPointGizmo.ControlPointGizmoStyle
import endorh.unican.gcrv.scene.property.Vec2fProperty
import endorh.unican.gcrv.scene.property.cubicSplineStyle
import endorh.unican.gcrv.scene.property.pointStyle
import endorh.unican.gcrv.scene.property.priority
import endorh.unican.gcrv.ui2.TRANSPARENT
import endorh.unican.gcrv.util.minus
import endorh.unican.gcrv.util.plus
import endorh.unican.gcrv.util.times
import endorh.unican.gcrv.util.toVec2i

class CubicSplineObject2D(
   points: List<Vec2f> = emptyList(),
   style: CubicSplineStyle = CubicSplineStyle(Color.WHITE),
   startStyle: PointStyle = PointStyle(Color.WHITE, 5F),
   midStyle: PointStyle = PointStyle(Color.TRANSPARENT, 0F),
   endStyle: PointStyle = PointStyle(Color.WHITE, 5F),
) : Object2D(Type) {
   val controlPoints by geoList(*points.toTypedArray())
   val style by cubicSplineStyle(style) priority -20
   val start by pointStyle(startStyle) priority -20
   val mid by pointStyle(midStyle) priority -20
   val end by pointStyle(endStyle) priority -20

   override val geometricCenter: Vec2f get() = controlPoints.geometricCenter
   override val renderers: List<Renderer2D> = listOf(Renderer(this))

   init {
      controlPoints.onChange { _gizmos = null }
   }
   private var _gizmos: List<Gizmo2D>? = null
   override val gizmos: List<Gizmo2D> get() {
      _gizmos?.let { return it }
      return (listOf(drawGizmo {
         val ps = controlPoints.entries
         val style = style.polygonStyle
         renderLine(Line2D(ps[0].value.toVec2i(), ps[1].value.toVec2i(), style))
         for (i in 2..ps.size - 2 step 3) {
            val mid = ps[i + 1].value.toVec2i()
            renderLine(Line2D(ps[i].value.toVec2i(), mid, style))
            if (i < ps.size - 2)
            renderLine(Line2D(mid, ps[i+2].value.toVec2i(), style))
         }
      }) + controlPoints.entries.mapIndexed { i, p ->
         gizmo(p, when (i) {
            0 -> startGizmoStyle
            controlPoints.size - controlPoints.size % 3 -> endGizmoStyle
            else -> if (i > controlPoints.size - controlPoints.size % 3)
               outGizmoStyle else if (i % 3 == 0) midGizmoStyle else controlGizmoStyle
         })
      }).also { _gizmos = it }
   }

   protected open val startGizmoStyle = ControlPointGizmoStyle(
      PointStyle(Color.RED, 15F, CircleAntiAliasPointRenderer))
   protected open val controlGizmoStyle = ControlPointGizmoStyle(
      PointStyle(Color.LIGHT_GRAY.withAlpha(0.7F), 13F, CircleAntiAliasPointRenderer))
   protected open val midGizmoStyle = ControlPointGizmoStyle(
      PointStyle(Color.GREEN, 11F, SquarePointRenderer))
   protected open val endGizmoStyle = ControlPointGizmoStyle(
      PointStyle(Color.BLUE, 15F, CircleAntiAliasPointRenderer))
   protected open val outGizmoStyle = ControlPointGizmoStyle(
      PointStyle(Color.GRAY, 13F, CircleAntiAliasPointRenderer))

   class Renderer(val spline: CubicSplineObject2D) : Renderer2D {
      override fun CubicSplineRenderPassInputScope.renderCubicSplines() {
         val ps = spline.controlPoints
         for (i in 0 until ps.size - 3 step 3)
            accept(CubicSpline2D(
               ps[i].value, ps[i+1].value, ps[i+2].value, ps[i+3].value, spline.style.cubicSplineStyle))
      }

      override fun PointRenderPassInputScope.renderPoints() {
         val ps = spline.controlPoints
         if (ps.size > 0) accept(Point2D(ps[0].value.toVec2i(), spline.style.startStyle))
         val mid = spline.style.midStyle
         for (i in 1..ps.size - 3 step 3) {
            // accept(Point2D(ps[i].value.toVec2i(), mid))
            // accept(Point2D(ps[i+1].value.toVec2i(), mid))
            accept(Point2D(ps[i+2].value.toVec2i(), if (i < ps.size - 3) mid else spline.style.endStyle))
         }
      }
   }

   object Type : Object2DType<CubicSplineObject2D>("cubic-spline") {
      private var splineCount = 0
      override fun generateName() = "Spline ${++splineCount}"
      override fun create() = CubicSplineObject2D()

      override val objectDrawer = object : ObjectDrawer<CubicSplineObject2D> {
         val style = mutableStateOf(CubicSplineStyle())
         var count = 0
         var preDragged: Vec2fProperty? = null
         var dragAnchor: Vec2f = Vec2f.ZERO
         var dragged: Vec2fProperty? = null
         var nextControl: Vec2fProperty? = null
         var nextPoint: Vec2fProperty? = null
         override fun ObjectDrawingContext<CubicSplineObject2D>.init() {
            count = 0
            drawnObject.controlPoints.clear()
         }

         private fun ObjectDrawingContext<CubicSplineObject2D>.add(pos: Vec2f? = null) =
            drawnObject.controlPoints.insert().also { point -> pos?.let { point.value = pos } }
         override fun ObjectDrawingContext<CubicSplineObject2D>.onDragStart(pos: Vec2f) {
            if (count == 0) {
               add(pos)
               dragged = add(pos)
            } else {
               preDragged = nextControl
               dragAnchor = nextPoint?.value ?: Vec2f.ZERO
               dragged = add(pos)
               nextControl = null
               nextPoint = null
            }
            count++
            update()
         }

         override fun ObjectDrawingContext<CubicSplineObject2D>.onDrag(pos: Vec2f) {
            dragged?.let { it.value = pos } ?: return
            preDragged?.let { it.value = dragAnchor * 2F - pos }
            update()
         }

         override fun ObjectDrawingContext<CubicSplineObject2D>.onHover(pos: Vec2f) {
            nextControl?.let { it.value = pos }
            nextPoint?.let { it.value = pos }
            update()
         }

         override fun ObjectDrawingContext<CubicSplineObject2D>.onDragEnd(pos: Vec2f) {
            nextControl = add(pos)
            nextPoint = add(pos)
            preDragged = null
            dragged = null
            count++
            update()
         }

         override fun ObjectDrawingContext<CubicSplineObject2D>.onRightClick() {
            val ps = drawnObject.controlPoints
            if (ps.size >= 4) {
               finish()
               if (nextPoint != null) {
                  ps.remove()
                  ps.remove()
               }
               for (i in 0..<(ps.size - 1) % 3) ps.remove()
            }
         }

         override fun UiScope.styleEditor(obj: CubicSplineObject2D?) {
            CubicSplineStyleEditor(style.use(), {
               style.value = it
               obj?.style?.cubicSplineStyle = it
            })
         }
      }
   }
}