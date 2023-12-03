package endorh.unican.gcrv.renderers.spline

import de.fabmax.kool.math.MutableVec2f
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.util.toVec2i
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable @SerialName("debug")
object DebugSplineRenderer : CubicSpline2DRenderer {
   override val name = "Debug"

   override fun PixelRendererContext.render(spline: CubicSpline2D) {
      val lineStyle = LineStyle(spline.style.color.withAlpha(0.5F), 1F, BresenhamRenderer)
      renderLine(Line2D(spline.p0.toVec2i(), spline.p1.toVec2i(), lineStyle))
      renderLine(Line2D(spline.p1.toVec2i(), spline.p2.toVec2i(), lineStyle))
      renderLine(Line2D(spline.p2.toVec2i(), spline.p3.toVec2i(), lineStyle))
      renderPoint(Point2D(spline.p0.toVec2i(), spline.style.startStyle))
      renderPoint(Point2D(spline.p1.toVec2i(), spline.style.midStyle))
      renderPoint(Point2D(spline.p2.toVec2i(), spline.style.midStyle))
      renderPoint(Point2D(spline.p3.toVec2i(), spline.style.endStyle))

      val p = MutableVec2f()
      for (t in 0..100) {
         spline.valueAt(t / 100F, p)
         plotPixel(p.x.roundToInt(), p.y.roundToInt())
      }
   }
}