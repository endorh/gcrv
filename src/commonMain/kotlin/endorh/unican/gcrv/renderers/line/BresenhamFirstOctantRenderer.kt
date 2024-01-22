package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.LineSegment2i
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("bresenham-first-octant")
object BresenhamFirstOctantRenderer : Line2DRenderer {
   override val name = "Bresenham (1st octant only)"
   override fun PixelRendererContext.render(line: LineSegment2i) {
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