package endorh.unican.gcrv.renderers.point

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.Point2i
import endorh.unican.gcrv.scene.Point2DRenderer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable @SerialName("tilted-square")
object TiltedSquarePointRenderer : Point2DRenderer {
   override val name = "Tilted Square"

   override fun PixelRendererContext.render(point: Point2i) {

      val size = point.style.size.roundToInt()
      val hs = size / 2
      val sX = point.x - hs
      val sY = point.y - hs
      for (x in sX..<sX + size) for (y in sY..<sY + size) {
         if (abs(x - point.x) + abs(y - point.y) <= hs)
            plotPixel(x, y)
      }
   }
}