package endorh.unican.gcrv.fractals.renderers

import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalStepResult
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.scene.objects.PolyLineObject2D
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.util.Rect2f
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.minus
import endorh.unican.gcrv.util.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.sqrt

@Serializable @SerialName("peano-curve")
object PeanoCurveRenderer : RecursiveGeoFractalRenderer {
   private val SQRT_3 = sqrt(3F)
   private val SQRT_3_6 = SQRT_3 / 6F

   override val displayName get() = "Peano Curve"
   override val initialGeometry = listOf(
      PolyLineObject2D(listOf(
         Vec2f(-0.5F, -0.5F), Vec2f(-0.5F, 0.5F),
         Vec2f(0.5F, 0.5F), Vec2f(0.5F, -0.5F))))
   override val finalStepAsOutput: Boolean get() = true

   override fun step(geometry: Object2D, step: Int, subStep: Int) = when (geometry) {
      is PolyLineObject2D -> {
         val a = geometry.points[0].value
         val b = geometry.points[1].value
         val c = geometry.points[2].value
         val d = geometry.points[3].value
         val uu = (b - a) / 4F
         val rr = (d - a) / 4F
         val nextGeometry = listOf(
            PolyLineObject2D(listOf(a - rr + uu, a + rr + uu, a + rr - uu, a - rr - uu)),
            LineObject2D(a - rr + uu, b - rr - uu),
            PolyLineObject2D(listOf(b - rr - uu, b - rr + uu, b + rr + uu, b + rr - uu)),
            LineObject2D(c - rr - uu, b + rr - uu),
            PolyLineObject2D(listOf(c - rr - uu, c - rr + uu, c + rr + uu, c + rr - uu)),
            LineObject2D(c + rr - uu, d + rr + uu),
            PolyLineObject2D(listOf(d + rr - uu, d - rr - uu, d - rr + uu, d + rr + uu)))
         RecursiveGeoFractalStepResult(emptyList(), nextGeometry)
      }
      is LineObject2D -> {
         val dd = (geometry.end - geometry.start) / 4F
         val ort = Vec2f(-dd.y, dd.x)
         RecursiveGeoFractalStepResult(emptyList(), listOf(
            LineObject2D(geometry.start + dd + ort, geometry.end - dd + ort)))
      }
      else -> throw IllegalStateException("Unexpected geometry type: ${geometry::class.simpleName}")
   }

   override fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean {
      val c = geometry.geometricCenter
      val p = (geometry as? PolyLineObject2D)?.points?.entries?.first()?.value ?: (geometry as LineObject2D).start
      val pp = viewport.clamp(c)
      val d = abs(pp.x - c.x + pp.y - c.y)
      val r = abs(p.x - c.x + p.y - c.y)
      return d < r * 2F
   }
}