package endorh.unican.gcrv.renderers.spline

import de.fabmax.kool.math.MutableVec2f
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.util.toVec2i
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable @SerialName("variable-interpolation-anti-alias")
object VariableInterpolationAntiAliasSplineRenderer : CubicSpline2DRenderer {
   override val name = "Variable Interpolation (antialiasing)"

   override fun PixelRendererContext.render(spline: CubicSpline2D) {
      val p = MutableVec2f(spline.p0)
      val last = MutableVec2f(p)
      var t = 0F
      var step = 0.001F
      while (t <= 1F) {
         t += step
         spline.valueAt(t, p)
         while (last == p) {
            t += step
            step *= 2F
            spline.valueAt(t, p)
         }
         while (p.sqrDistance(last) > 1F && step > 1e-6F) {
            step /= 2F
            t -= step
            spline.valueAt(t, p)
         }
         plotPixel(p.x.roundToInt(), p.y.roundToInt())
         last.set(p)
      }
   }
}