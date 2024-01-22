package endorh.unican.gcrv.fractals.renderers

import de.fabmax.kool.math.MutableVec2f
import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalStepResult
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable @SerialName("koch-curve")
object KochCurveRenderer : RecursiveGeoFractalRenderer {
   private val SQRT_3 = sqrt(3F)
   private val SQRT_3_6 = SQRT_3 / 6F

   override val displayName get() = "Koch Curve"
   override val initialGeometry = listOf(
      LineObject2D(Vec2f(-0.5F, 0F), Vec2f(0.5F, 0F)))
   override val finalStepAsOutput: Boolean get() = true

   override fun step(geometry: Object2D, step: Int, subStep: Int): RecursiveGeoFractalStepResult {
      val line = geometry as LineObject2D
      val s = line.start
      val e = line.end
      val sse = (s * 2F + e) / 3F
      val see = (s + e * 2F) / 3F
      val dir = (e - s)
      val ort = Vec2f(-dir.y, dir.x)
      val m = (s + e) / 2F + (ort * SQRT_3_6)
      val nextGeometry = listOf(
         LineObject2D(s, sse),
         LineObject2D(sse, m),
         LineObject2D(m, see),
         LineObject2D(see, e))
      return RecursiveGeoFractalStepResult(emptyList(), nextGeometry)
   }

   override fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean {
      val line = geometry as LineObject2D
      val p = MutableVec2f(line.end)
      p.subtract(line.start)
      val x = p.x
      p.x = -p.y
      p.y = x
      p *= SQRT_3_6
      p.x += (line.start.x + line.end.x) / 2F
      p.y += (line.start.y + line.end.y) / 2F
      return listOf(line.start, line.end, p).boundingBox().intersectionRect(viewport).isNotEmpty()
   }
}