package endorh.unican.gcrv.line_algorithms.renderers.point

import de.fabmax.kool.math.Vec2i
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.line_algorithms.Point2DRenderer

object SquarePointRenderer : Point2DRenderer {
   override val name = "Square"

   override fun PixelRendererContext.render(point: Vec2i, size: Int) {
      val hs = size / 2
      val sX = point.x - hs
      val sY = point.y - hs
      for (x in sX..<sX + size) for (y in sY..<sY + size)
         plotPixel(x, y)
   }
}