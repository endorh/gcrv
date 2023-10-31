package endorh.unican.gcrv.line_algorithms.renderers.line

import endorh.unican.gcrv.line_algorithms.Line2D
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.util.towards
import kotlin.math.abs

object BresenhamRenderer : Line2DRenderer {
   override val name = "Bresenham (all octants)"

   override fun PixelRendererContext.render(line: Line2D) {
      val (xS, yS, xE, yE) = line.coords

      val dx = abs(xE - xS)
      val dy = abs(yE - yS)


      if (dy <= dx) {
         val sy = if (yS < yE) 1 else -1

         var y = yS
         var err = 2 * dy - dx
         for (x in xS towards xE) {
            plotPixel(x, y)
            if (err > 0) {
               y += sy
               err -= 2 * dx
            }
            err += 2 * dy
         }
      } else {
         val sx = if (xS < xE) 1 else -1

         var x = xS
         var err = 2 * dx - dy
         for (y in yS towards yE) {
            plotPixel(x, y)
            if (err > 0) {
               x += sx
               err -= 2 * dy
            }
            err += 2 * dx
         }
      }
   }
}