package endorh.unican.gcrv.line_algorithms.renderers.point

import de.fabmax.kool.math.Vec2i
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.line_algorithms.Point2DRenderer
import endorh.unican.gcrv.util.component1
import endorh.unican.gcrv.util.component2

object CirclePointRenderer : Point2DRenderer {
   override val name = "Circle"

   override fun PixelRendererContext.render(point: Vec2i, size: Int) {
      val hs = size / 2
      val (x, y) = point
      val hs2 = hs * hs
      for (i in -hs..<size - hs) for (j in -hs..<size - hs) {
         if (i*i + j*j <= hs2)
            plotPixel(x + i, y + j)
      }
   }
}