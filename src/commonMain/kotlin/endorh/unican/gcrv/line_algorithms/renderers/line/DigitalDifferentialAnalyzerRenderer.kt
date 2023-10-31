package endorh.unican.gcrv.line_algorithms.renderers.line

import endorh.unican.gcrv.line_algorithms.Line2D
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import kotlin.math.abs
import kotlin.math.floor

object DigitalDifferentialAnalyzerRenderer : Line2DRenderer {
   override val name = "DDA (Digital Differential Analyzer)"

   override fun PixelRendererContext.render(line: Line2D) {
      val (xS, yS, xE, yE) = line.coords
      val dx = xE - xS
      val dy = yE - yS

      val m = maxOf(abs(dx), abs(dy))

      val dxNorm = dx.F / m
      val dyNorm = dy.F / m

      var x = xS + 0.5F
      var y = yS + 0.5F

      for (i in 0..m) {
         plotPixel(floor(x).I, floor(y).I)
         x += dxNorm
         y += dyNorm
      }
   }
}