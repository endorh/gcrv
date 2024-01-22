package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.LineSegment2i
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("slope-intercept")
object SlopeInterceptRenderer : Line2DRenderer {
   override val name = "Slope-Intercept"

   override fun PixelRendererContext.render(line: LineSegment2i) {
      val (xS, yS, xE, _) = line.coords

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