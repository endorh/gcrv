package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.Line2D
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("slope-one")
object SlopeOneLineRenderer : Line2DRenderer {
   override val name = "45-deg (only slope ±1)"

   override fun PixelRendererContext.render(line: Line2D) {
      val (left, right) = line.leftToRight
      val dy = if (line.isSlopePositive) 1 else -1

      var y = left.y
      for (x in left.x..right.x) {
         plotPixel(x, y)
         y += dy
      }
   }
}