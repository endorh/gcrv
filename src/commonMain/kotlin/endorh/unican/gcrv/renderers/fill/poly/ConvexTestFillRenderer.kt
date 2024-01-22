package endorh.unican.gcrv.renderers.fill.poly

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.PolyFill2DRenderer
import endorh.unican.gcrv.scene.PolyFill2f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("convex-test-poly-fill")
object ConvexTestFillRenderer : PolyFill2DRenderer {
   override val name get() = "Convex Test Fill"

   override fun PixelRendererContext.render(fill: PolyFill2f) {
      val bb = fill.boundingBox.growToRect2i()
      val semiPlanes = fill.semiPlanes
      withColor(fill.style.color) {
         for (p in bb.pixels) {
            if (semiPlanes.all { p in it }) {
               plotPixel(p.x, p.y)
            }
         }
      }
   }
}