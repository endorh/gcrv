package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.LineSegment2i
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.towards
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable @SerialName("slope-intercept-ex")
object ExtendedSlopeInterceptRenderer : Line2DRenderer {
   override val name = "Modified Slope-Intercept"

   override fun PixelRendererContext.render(line: LineSegment2i) {
      val (xL, yL, xR, yR) = line.leftToRightCoords

      val dx = xR - xL
      val dy = yR - yL

      if (abs(dy) < dx) {
         val slope = dy.F / dx.F
         val yIntercept = yL - slope * xL
         plotPixel(xL, yL)
         for (x in xL + 1 towards xR) {
            val y = (slope * x + yIntercept).roundToInt()
            plotPixel(x, y)
         }
      } else {
         val slopeInv = dx.F / dy.F
         val xIntercept = xL - slopeInv * yL
         plotPixel(xL, yL)
         for (y in yL + 1 towards yR) {
            val x = (slopeInv * y + xIntercept).roundToInt()
            plotPixel(x, y)
         }
      }
   }
}