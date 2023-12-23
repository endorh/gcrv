package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.MutableVec2f
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
import endorh.unican.gcrv.scene.ControlPointGizmo.GizmoDragListener
import endorh.unican.gcrv.scene.property.*
import endorh.unican.gcrv.ui2.TRANSPARENT
import endorh.unican.gcrv.util.*

open class CubicSplineObject2D(
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
      controlPoints.onChange {
         _gizmos?.forEach {
            if (it is ControlPointGizmo) (it.onChange as? SplineGizmoDragListener)?.invalidate()
         }
         _gizmos = null
      }
   }
   private var _gizmos: List<Gizmo2D>? = null
   override val gizmos: List<Gizmo2D> get() {
      _gizmos?.let { return it }
      return (listOf(drawGizmo { transform ->
         val ps = controlPoints.entries
         val style = style.polygonStyle
         renderLine(Line2D((transform * ps[0].value).toVec2i(), (transform * ps[1].value).toVec2i(), style))
         for (i in 2..ps.size - 2 step 3) {
            val mid = (transform * ps[i + 1].value).toVec2i()
            renderLine(Line2D((transform * ps[i].value).toVec2i(), mid, style))
            if (i < ps.size - 2)
            renderLine(Line2D(mid, (transform * ps[i+2].value).toVec2i(), style))
         }
      }) + controlPoints.entries.mapIndexed { i, p ->
         gizmo(p, when (i) {
            0 -> startGizmoStyle
            controlPoints.size - controlPoints.size % 3 -> endGizmoStyle
            else -> if (i > controlPoints.size - controlPoints.size % 3)
               outGizmoStyle else if (i % 3 == 0) midGizmoStyle else controlGizmoStyle
         }, SplineGizmoDragListener(i, controlPoints))
      }).also { _gizmos = it }
   }

   protected open class SplineGizmoDragListener(
      val idx: Int, val controlPoints: PropertyList<AnimPropertyData<Vec2f>, Vec2fProperty>
   ) : GizmoDragListener {
      protected var valid = true
      fun invalidate() {
         valid = false
         ModifierState.removeListener(modifierListener)
      }
      protected val modifierListener = ModifierChangeListener {
         onDrag(lastPos)
      }
      protected val endIndex = controlPoints.size - controlPoints.size % 3
      init {
         if (idx > endIndex) invalidate()
      }
      lateinit var initPos: Vec2f
      lateinit var initPositions: List<Vec2f>
      var connected: Boolean = false
      lateinit var lastPos: Vec2f
      override fun onDragStart(pos: Vec2f) {
         if (!valid) return
         initPos = pos
         connected = controlPoints[0].value.sqrDistance(controlPoints[endIndex].value) < 16F
         if (connected) {
            if (idx == 0) controlPoints[endIndex].value = controlPoints[0].value
            if (idx == endIndex) controlPoints[0].value = controlPoints[endIndex].value
         }
         initPositions = controlPoints.entries.map { it.value }
         lastPos = pos
         ModifierState.addListener(modifierListener)
      }
      override fun onDrag(pos: Vec2f) {
         if (!valid) return
         if (ModifierState.ctrlPressed) {
            controlPoints.entries.forEachIndexed { i, p ->
               if (p.value != initPositions[i])
                  p.value = initPositions[i]
            }
            controlPoints[idx].value = pos
         } else {
            val delta = pos - initPos
            fun dragPoints(vararg indices: Int) = indices.forEach {
               controlPoints[it].value = initPositions[it] + delta
            }
            if (idx == 0) {
               dragPoints(*if (connected) intArrayOf(0, 1, endIndex - 1, endIndex) else intArrayOf(0, 1))
            } else if (idx == endIndex) {
               dragPoints(*if (connected) intArrayOf(endIndex - 1, endIndex, 0, 1) else intArrayOf(endIndex - 1, endIndex))
            } else if (idx % 3 == 0) dragPoints(idx - 1, idx, idx + 1) else {
               controlPoints[idx].value = pos
               var o = if (idx % 3 == 1) idx - 2 else idx + 2
               var pivot = if (idx % 3 == 1) idx - 1 else idx + 1
               if (o < 0) {
                  if (connected) {
                     controlPoints[endIndex].value = initPositions[0]
                     o = endIndex - 1
                     pivot = endIndex
                  }
               } else if (o > endIndex) {
                  if (connected) {
                     controlPoints[0].value = initPositions[endIndex]
                     o = 1
                     pivot = 0
                  } else o = -1
               }
               if (o > 0) {
                  if (ModifierState.shiftPressed) {
                     controlPoints[o].value = initPositions[pivot] * 2F - pos
                  } else {
                     val diff = initPositions[pivot] - pos
                     val offset = diff * (initPositions[pivot].distance(initPositions[o]) / diff.length())
                     controlPoints[o].value = initPositions[pivot] + offset
                  }
               }
            }
         }
         lastPos = pos
      }
      override fun onDragEnd(pos: Vec2f) {
         if (!valid) return
         ModifierState.removeListener(modifierListener)
         lastPos = pos
         initPositions = emptyList()
      }
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