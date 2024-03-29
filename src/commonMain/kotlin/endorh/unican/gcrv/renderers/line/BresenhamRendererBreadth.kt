package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.LineSegment2i
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.util.towards
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable @SerialName("bresenham-breadth")
object BresenhamRendererBreadth : Line2DRenderer {
   override val name = "Bresenham (with breadth)"

   override fun PixelRendererContext.render(line: LineSegment2i) {
      val width = line.style.breadth.roundToInt()
      if (width == 0) return
      val (xS, yS, xE, yE) = line.coords

      val dx = abs(xE - xS)
      val dy = abs(yE - yS)

      if (dy <= dx) {
         val sy = if (yS < yE) 1 else -1

         for (bdy in -width / 2..<width - width / 2) {
            var y = yS + bdy
            var err = 2 * dy - dx
            for (x in xS towards xE) {
               plotPixel(x, y)
               if (err > 0) {
                  y += sy
                  err -= 2 * dx
               }
               err += 2 * dy
            }
         }
      } else {
         val sx = if (xS < xE) 1 else -1

         for (bdx in -width / 2..<width - width / 2) {
            var x = xS + bdx
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
}