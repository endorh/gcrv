package endorh.unican.gcrv.renderers.spline

import de.fabmax.kool.math.MutableVec2f
import endorh.unican.gcrv.scene.CubicSpline2f
import endorh.unican.gcrv.scene.CubicSpline2DRenderer
import endorh.unican.gcrv.scene.PixelRendererContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable @SerialName("variable-interpolation")
object VariableInterpolationSplineRenderer : CubicSpline2DRenderer {
   override val name = "Variable Interpolation"

   override fun PixelRendererContext.render(spline: CubicSpline2f) {
      val p = MutableVec2f(spline.p0)
      val last = MutableVec2f(p)
      var t = 0F
      var step = 0.001F
      while (t <= 1F) {
         t += step
         spline.valueAt(t, res=p)
         while (last == p) {
            t += step
            step *= 2F
            spline.valueAt(t, res=p)
         }
         while (p.sqrDistance(last) > 1F && step > 1e-6F) {
            step /= 2F
            t -= step
            spline.valueAt(t, res=p)
         }
         if (!p.x.isNaN() && !p.y.isNaN()) {
            plotPixel(p.x.roundToInt(), p.y.roundToInt())
         } else break
         last.set(p)
      }
   }
}