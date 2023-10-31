package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.line_algorithms.renderers.point.CirclePointRenderer
import endorh.unican.gcrv.ui2.BufferCanvas

fun interface RenderPass2D {
   fun render(canvas: BufferCanvas, objectStack: Object2DStack)
}

data class WireframeRenderingSettings(
   var fallbackLineRenderer: MutableStateValue<Line2DRenderer> = mutableStateOf(BresenhamRenderer),
   var enforceLineRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   var fallbackLineColor: MutableStateValue<Color> = mutableStateOf(Color.WHITE),
   var enforceLineColor: MutableStateValue<Boolean> = mutableStateOf(false),
   var fallbackPointRenderer: MutableStateValue<Point2DRenderer> = mutableStateOf(CirclePointRenderer),
   var enforcePointRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   var startPointColor: MutableStateValue<Color> = mutableStateOf(Color.RED),
   var endPointColor: MutableStateValue<Color> = mutableStateOf(Color.BLUE),
   var enforcePointColor: MutableStateValue<Boolean> = mutableStateOf(false),
   var pointSize: MutableStateValue<Int> = mutableStateOf(0),
)

class WireframeRenderPass2D(val settings: WireframeRenderingSettings = WireframeRenderingSettings()) : RenderPass2D {
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      canvas.update {
         for (o in objectStack.objects) for (line in o.wireLines) {
            with(
               if (settings.enforceLineRenderer.value) settings.fallbackLineRenderer.value
               else line.style.renderer ?: settings.fallbackLineRenderer.value
            ) {
               rendererContext.color =
                  if (settings.enforceLineColor.value) settings.fallbackLineColor.value
                  else line.style.color
               rendererContext.render(line)
            }
            if (settings.pointSize.value > 0) with(settings.fallbackPointRenderer.value) {
               rendererContext.color =
                  if (settings.enforcePointColor.value) settings.startPointColor.value
                  else line.style.color
               rendererContext.render(line.start, settings.pointSize.value)
               rendererContext.color =
                  if (settings.enforcePointColor.value) settings.endPointColor.value
                  else line.style.color
               rendererContext.render(line.end, settings.pointSize.value)
            }
         }
      }
   }
}

class AxesRenderPass2D : RenderPass2D {
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      canvas.update {
         for (x in canvas.origin.x..<canvas.origin.x + canvas.width)
            canvas.F.C[x, 0] = Color.GRAY
         for (y in canvas.origin.y..<canvas.origin.y + canvas.height)
            canvas.F.C[0, y] = Color.GRAY
      }
   }
}

class RenderingPipeline2D(var objectStack: Object2DStack, passes: Collection<RenderPass2D> = emptyList()) {
   val renderPasses = passes.toMutableList()

   fun render(canvas: BufferCanvas) {
      for (pass in renderPasses)
         pass.render(canvas, objectStack)
   }

   fun clear(canvas: BufferCanvas) = canvas.clear()
}