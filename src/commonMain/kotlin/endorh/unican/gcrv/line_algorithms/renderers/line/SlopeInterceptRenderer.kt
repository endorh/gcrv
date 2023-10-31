package endorh.unican.gcrv.line_algorithms.renderers.line

import endorh.unican.gcrv.line_algorithms.Line2D
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.PixelRendererContext

object SlopeInterceptRenderer : Line2DRenderer {
   override val name = "Slope-Intercept"

   override fun PixelRendererContext.render(line: Line2D) {
      val (xS, yS, xE, yE) = line.coords

      val slope = line.slope
      val yIntercept = line.yIntercept

      var y = yS
      plotPixel(xS, y)
      for (x in xS + 1..xE) {
         y = (slope * x + yIntercept).toInt()
         plotPixel(x, y)
      }
   }
}