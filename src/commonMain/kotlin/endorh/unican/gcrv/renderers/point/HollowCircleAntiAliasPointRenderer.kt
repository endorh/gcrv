package endorh.unican.gcrv.renderers.point

import endorh.unican.gcrv.scene.PixelRendererContext
import endorh.unican.gcrv.scene.Point2i
import endorh.unican.gcrv.scene.Point2DRenderer
import endorh.unican.gcrv.util.component1
import endorh.unican.gcrv.util.component2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Determina la transparencia de los píxeles del borde en función de la distancia
 * del centro del pixel al borde de la circunferencia.
 */
@Serializable @SerialName("hollow-circle-anti-alias")
object HollowCircleAntiAliasPointRenderer : Point2DRenderer {
   override val name = "Hollow Circle (antialiased)"

   override fun PixelRendererContext.render(point: Point2i) {
      val size = point.style.size.roundToInt()
      val hs = size / 2
      val ihs = hs * 4 / 5
      val (x, y) = point.pos
      val hs2 = hs * hs
      val ihs2 = ihs * ihs
      val iihs2 = (ihs - 1) * (ihs - 1)
      for (i in 0..<size - hs) for (j in 0..<size - hs) {
         val ij2 = i*i + j*j
         if (ij2 < iihs2) {
            continue
         } else if (ij2 < ihs2) {
            val ci = i + 0.5F
            val cj = j + 0.5F
            val alpha = 1F - (sqrt(ci*ci + cj*cj) - ihs + SQRT_2_HALF) / SQRT_2
            plotPixel(x + i, y + j, alpha)
            if (i != 0) plotPixel(x - i, y + j, alpha)
            if (j != 0) plotPixel(x + i, y - j, alpha)
            if (i != 0 && j != 0) plotPixel(x - i, y - j, alpha)
         } else if (ij2 <= hs2) {
            plotPixel(x + i, y + j)
            if (i != 0) plotPixel(x - i, y + j)
            if (j != 0) plotPixel(x + i, y - j)
            if (i != 0 && j != 0) plotPixel(x - i, y - j)
         } else {
            val ii = i - 1
            val jj = j - 1
            if (ii*ii + jj*jj <= hs2) {
               val ci = i - 0.5F
               val cj = j - 0.5F
               val alpha = 1F - (sqrt(ci * ci + cj * cj) - hs + SQRT_2_HALF) / SQRT_2
               plotPixel(x + i, y + j, alpha)
               if (i != 0) plotPixel(x - i, y + j, alpha)
               if (j != 0) plotPixel(x + i, y - j, alpha)
               if (i != 0 && j != 0) plotPixel(x - i, y - j, alpha)
            }
         }
      }
   }
}