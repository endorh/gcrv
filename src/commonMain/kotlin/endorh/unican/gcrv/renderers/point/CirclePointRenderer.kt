package endorh.unican.gcrv.renderers.point

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.Point2i
import endorh.unican.gcrv.scene.Point2DRenderer
import endorh.unican.gcrv.util.component1
import endorh.unican.gcrv.util.component2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable @SerialName("circle")
object CirclePointRenderer : Point2DRenderer {
   override val name = "Circle"

   override fun PixelRendererContext.render(point: Point2i) {
      val size = point.style.size.roundToInt()
      val hs = size / 2
      val (x, y) = point.pos
      val hs2 = hs * hs
      for (i in -hs..<size - hs) for (j in -hs..<size - hs) {
         if (i*i + j*j <= hs2)
            plotPixel(x + i, y + j)
      }
   }
}