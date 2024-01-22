package endorh.unican.gcrv.fractals.renderers

import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalStepResult
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @SerialName("dragon-nail")
object DragonNailRenderer : RecursiveGeoFractalRenderer {
   override val displayName get() = "Dragon Nail"
   override val initialGeometry = listOf(
      LineObject2D(Vec2f(-0.5F, 0F), Vec2f(0.5F, 0F)))
   override val finalStepAsOutput: Boolean get() = true

   override fun step(geometry: Object2D, step: Int, subStep: Int): RecursiveGeoFractalStepResult {
      val line = geometry as LineObject2D
      val s = line.start
      val e = line.end
      val dir = (e - s)
      val ort = Vec2f(-dir.y, dir.x)
      val m = (s + e + ort) / 2F
      val nextGeometry = listOf(
         LineObject2D(m, s),
         LineObject2D(m, e))
      return RecursiveGeoFractalStepResult(emptyList(), nextGeometry)
   }

   override fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean {
      val line = geometry as LineObject2D
      val r = (line.end.x - line.start.x + line.end.y - line.start.y) * 2F
      return viewport.intersectsCircle((line.start + line.end) / 2F, r)
   }
}