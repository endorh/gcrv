package endorh.unican.gcrv.renderers.fill.poly

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.PolyFill2DRenderer
import endorh.unican.gcrv.scene.PolyFill2f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("no-op-poly-fill")
object NoOpFillRenderer : PolyFill2DRenderer {
   override val name get() = "No Fill"
   override fun PixelRendererContext.render(fill: PolyFill2f) {}
}