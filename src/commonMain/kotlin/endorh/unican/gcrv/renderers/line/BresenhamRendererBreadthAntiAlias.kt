package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.Line2D
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.towards
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable @SerialName("bresenham-breadth-anti-alias")
object BresenhamRendererBreadthAntiAlias : Line2DRenderer {
   override val name = "Bresenham (antialiasing)"

   override fun PixelRendererContext.render(line: Line2D) {
      val width = line.style.breadth.roundToInt() + 2
      val hw = width / 2

      val (xS, yS, xE, yE) = line.coords

      val dx = abs(xE - xS)
      val dy = abs(yE - yS)

      if (dy <= dx) {
         val sy = if (yS < yE) 1 else -1

         run {
            var y = yS - hw
            var err = -dx
            for (x in xS towards xE) {
               plotPixel(x, y, if (yS < yE) -err.F / (2 * dx).F else 1F + err.F / (2 * dx).F)
               err += 2 * dy
               if (err > 0) {
                  y += sy
                  err -= 2 * dx
               }
            }
         }
         for (bdy in 1 - hw..<width - hw - 1) {
            var y = yS + bdy
            var err = -dx
            for (x in xS towards xE) {
               plotPixel(x, y)
               err += 2 * dy
               if (err > 0) {
                  y += sy
                  err -= 2 * dx
               }
            }
         }
         run {
            var y = yS + width - hw - 1
            var err = -dx
            for (x in xS towards xE) {
               plotPixel(x, y, if (yS < yE) 1F + err.F / (2 * dx).F else -err.F / (2 * dx).F)
               err += 2 * dy
               if (err > 0) {
                  y += sy
                  err -= 2 * dx
               }
            }
         }
      } else {
         val sx = if (xS < xE) 1 else -1

         run {
            var x = xS - hw
            var err = -dy
            for (y in yS towards yE) {
               plotPixel(x, y, if (xS < xE) -err.F / (2 * dy).F else 1F + err.F / (2 * dy).F)
               err += 2 * dx
               if (err > 0) {
                  x += sx
                  err -= 2 * dy
               }
            }
         }

         for (bdx in 1 - hw..<width - hw - 1) {
            var x = xS + bdx
            var err = -dy
            for (y in yS towards yE) {
               plotPixel(x, y)
               err += 2 * dx
               if (err > 0) {
                  x += sx
                  err -= 2 * dy
               }
            }
         }

         run {
            var x = xS + width - hw - 1
            var err = -dy
            for (y in yS towards yE) {
               plotPixel(x, y, if (xS < xE) 1F + err.F / (2 * dy).F else -err.F / (2 * dy).F)
               err += 2 * dx
               if (err > 0) {
                  x += sx
                  err -= 2 * dy
               }
            }
         }
      }
   }
}