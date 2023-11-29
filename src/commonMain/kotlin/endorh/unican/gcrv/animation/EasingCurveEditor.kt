package endorh.unican.gcrv.animation

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.CanvasPixelRendererContext
import endorh.unican.gcrv.line_algorithms.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import kotlin.math.roundToInt

fun UiScope.EasingCurveEditor(
   easing: Easing,
   size: Int,
   curveColor: Color = Color.GRAY,
   borderColor: Color = Color.DARK_GRAY,
   gridColor: Color = Color.DARK_GRAY.withAlpha(0.7F),
   controlPointColor: Color = Color.LIGHT_GRAY,
   block: CanvasScope.() -> Unit
) {
   val panel = remember { EasingCurveEditorPanel(easing, size, curveColor, borderColor, gridColor, controlPointColor, block) }
   panel.setup(easing, size, curveColor, borderColor, gridColor, controlPointColor, block)
   panel()
}

class EasingCurveEditorPanel(
   var easing: Easing,
   var size: Int,
   var curveColor: Color = Color.GRAY,
   var borderColor: Color = Color.LIGHT_GRAY,
   var gridColor: Color = Color.DARK_GRAY.withAlpha(0.7F),
   var controlPointColor: Color = Color.LIGHT_GRAY,
   var block: CanvasScope.() -> Unit
) : Composable {
   var canvas = makeCanvas(size)
   var lastValues = easing.controlPoints.map { it.value }
   var draggedControlPoint = mutableSerialStateOf<ControlPoint?>(null)

   private fun makeCanvas(size: Int) = BufferCanvas(size, 2 * size).apply {
      origin.set(Vec2i(0, -size / 2))
      this@EasingCurveEditorPanel.size = size
   }

   init {
      drawCanvas()
   }

   fun setup(
      easing: Easing, size: Int,
      curveColor: Color, borderColor: Color, gridColor: Color,
      controlPointColor: Color, block: CanvasScope.() -> Unit
   ) {
      val values = easing.controlPoints.map { it.value }
      val dirty = this.easing != easing || this.size != size
        || this.curveColor != curveColor || this.gridColor != gridColor
        || this.controlPointColor != controlPointColor
        || lastValues != values
      if (dirty) lastValues = values
      this.easing = easing
      if (this.size != size) {
         canvas = makeCanvas(size)
         drawCanvas()
      }
      this.curveColor = curveColor
      this.borderColor = borderColor
      this.gridColor = gridColor
      this.controlPointColor = controlPointColor
      if (dirty) drawCanvas()
      this.block = block
   }

   override fun UiScope.compose() = Canvas(canvas, "ease-editor") {
      modifier
         .canvasSize(CanvasSize.FixedScale(1F))
         .invertY(true)
         .uvRect(UvRect.FULL)
         .onDragStart { ev ->
            val pos = ev.canvasPosition / size.F
            val m = 15F / size.F
            easing.controlPoints.asSequence().sortedBy {
               it.value.distance(pos)
            }.filter { it.value.distance(pos) <= m }.firstOrNull()?.let {
               draggedControlPoint.value = it
            }
         }
         .onDrag { ev ->
            draggedControlPoint.value?.let {
               val pos = ev.canvasPosition / size.F
               it.value = pos
               drawCanvas()
            }
         }.onDragEnd { ev ->
            draggedControlPoint.value?.let {
               val pos = ev.canvasPosition / size.F
               it.value = pos
               draggedControlPoint.value = null
               drawCanvas()
            }
         }
      block()
   }

   fun drawCanvas() {
      canvas.update {
         canvas.clear()

         val dimGrid = gridColor.withAlpha(0.5F)
         for (i in -5..15) {
            val t = (i.F / 10F * size.F).roundToInt()
            val c = if (i in 1..<10) gridColor else dimGrid
            for (x in 0..<size)
               canvas.F.C[x, t] = gridColor
            if (i in 1..<10) {
               for (y in -size / 2..<0)
                  canvas.F.C[t, y] = dimGrid
               for (y in 0..<size)
                  canvas.F.C[t, y] = gridColor
               for (y in size..<size * 3 / 2)
                  canvas.F.C[t, y] = dimGrid
            }
         }

         for (x in 0..<size) {
            canvas.F.C[x, 0] = borderColor
            canvas.F.C[x, 1] = borderColor
            canvas.F.C[x, size - 1] = borderColor
            canvas.F.C[x, size - 2] = borderColor
         }

         for (y in 0..<size) {
            canvas.F.C[0, y] = borderColor
            canvas.F.C[1, y] = borderColor
            canvas.F.C[size-1, y] = borderColor
            canvas.F.C[size-2, y] = borderColor
         }

         var lastY = 0
         val dim = curveColor.withAlpha(0.5F)
         val cornerDim = curveColor.withAlpha(0.2F)
         for (x in 0..<size) {
            val y = (easing.ease(x.F / size.F) * size.F).roundToInt()
            for (yy in lastY towards y) {
               for (i in -1..1) for (j in -1..1)
                  canvas.F.C.M[x+i, yy+j] = if (i == 0 && j == 0) curveColor
                  else if (i == 0 || j == 0) dim else cornerDim
            }
            lastY = y
         }

         with (CanvasPixelRendererContext(canvas)) {
            color = controlPointColor
            with(CircleAntiAliasPointRenderer) {
               render(Vec2i(0, 0), 9)
               render(Vec2i(size - 1, size - 1), 9)
            }

            with (easing) {
               drawGizmos(size)
            }

            for (c in easing.controlPoints) {
               val v = (c.value * size.F).toVec2i()
               with(CircleAntiAliasPointRenderer) {
                  color = controlPointColor
                  render(v, 15)
                  color = Color.DARK_GRAY
                  render(v, 9)
               }
            }
         }
      }
   }
}