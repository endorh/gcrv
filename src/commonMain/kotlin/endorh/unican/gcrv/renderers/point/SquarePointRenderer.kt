package endorh.unican.gcrv.renderers.point

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.Point2D
import endorh.unican.gcrv.scene.Point2DRenderer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable @SerialName("square")
object SquarePointRenderer : Point2DRenderer {
   override val name = "Square"

   override fun PixelRendererContext.render(point: Point2D) {
      val size = point.style.size.roundToInt()
      val hs = size / 2
      val sX = point.x - hs
      val sY = point.y - hs
      for (x in sX..<sX + size) for (y in sY..<sY + size)
         plotPixel(x, y)
   }
}