package endorh.unican.gcrv.line_algorithms.renderers.line

import endorh.unican.gcrv.line_algorithms.Line2D
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.util.F
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("bresenham-first-octant")
object BresenhamFirstOctantRenderer : Line2DRenderer {
   override val name = "Bresenham (1st octant only)"
   override fun PixelRendererContext.render(line: Line2D) {
      val (xS, yS, xE, yE) = line.coords

      val dx = xE - xS
      val dy = yE - yS

      var y = yS
      var err = 2 * dy - dx
      for (x in xS..xE) {
         plotPixel(x, y)
         if (err > 0) {
            y++
            err -= 2 * dx
         }
         err += 2 * dy
      }
   }
}