package endorh.unican.gcrv.line_algorithms.renderers.point

import de.fabmax.kool.math.Vec2i
import endorh.unican.gcrv.line_algorithms.PixelRendererContext
import endorh.unican.gcrv.line_algorithms.Point2DRenderer
import endorh.unican.gcrv.util.component1
import endorh.unican.gcrv.util.component2
import kotlin.math.sqrt

val SQRT_2 = sqrt(2F)
val SQRT_2_HALF = SQRT_2 / 2F

/**
 * Determina la transparencia de los píxeles del borde en función de la distancia
 * del centro del pixel al borde de la circunferencia.
 */
object CircleAntiAliasPointRenderer : Point2DRenderer {
   override val name = "Circle (antialiased)"

   override fun PixelRendererContext.render(point: Vec2i, size: Int) {
      val hs = size / 2
      val (x, y) = point
      val hs2 = hs * hs
      for (i in 0..<size - hs) for (j in 0..<size - hs) {
         if (i*i + j*j <= hs2) {
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
               val alpha = 1F - (sqrt(ci*ci + cj*cj) - hs + SQRT_2_HALF) / SQRT_2
               plotPixel(x + i, y + j, alpha)
               if (i != 0) plotPixel(x - i, y + j, alpha)
               if (j != 0) plotPixel(x + i, y - j, alpha)
               if (i != 0 && j != 0) plotPixel(x - i, y - j, alpha)
            }
         }
      }
   }
}