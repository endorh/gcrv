package endorh.unican.gcrv.renderers.line

import endorh.unican.gcrv.scene.Line2D
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("orthogonal")
object OrthogonalLineRenderer : Line2DRenderer {
   override val name = "Orthogonal (only horizontal/vertical lines)"

   override fun PixelRendererContext.render(line: Line2D) {
      val (start, end) = line
      if (line.isHorizontal) {
         val y = start.y
         for (x in start.x..end.x)
            plotPixel(x, y)
      } else {
         val x = start.x
         for (y in start.y..end.y)
            plotPixel(x, y)
      }
   }
}