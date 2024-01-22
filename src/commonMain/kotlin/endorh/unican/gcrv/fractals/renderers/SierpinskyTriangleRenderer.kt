package endorh.unican.gcrv.fractals.renderers

import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalStepResult
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.scene.objects.TriangleObject2D
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.util.Rect2f
import endorh.unican.gcrv.util.boundingBox
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable @SerialName("sierpinsky-triangle")
object SierpinskyTriangleRenderer : RecursiveGeoFractalRenderer {
   private val SQRT_3 = sqrt(3F)
   private val SQRT_3_2 = SQRT_3 / 2F

   override val displayName get() = "Sierpinsky Triangle"
   override val initialGeometry = listOf(
      TriangleObject2D(Vec2f(-0.5F, 0F), Vec2f(0.5F, 0F), Vec2f(0F, SQRT_3_2)))
   override val extraGeometry: List<Object2D> get() = initialGeometry
   override val finalStepAsOutput: Boolean get() = false

   override fun step(geometry: Object2D, step: Int, subStep: Int): RecursiveGeoFractalStepResult {
      val tri = geometry as TriangleObject2D
      val a = tri.a
      val b = tri.b
      val c = tri.c
      val ab = (a + b) / 2F
      val bc = (b + c) / 2F
      val ca = (c + a) / 2F
      val outputGeometry = listOf(
         LineObject2D(ab, bc),
         LineObject2D(bc, ca),
         LineObject2D(ca, ab))
      val nextGeometry = listOf(
         TriangleObject2D(a, ab, ca),
         TriangleObject2D(ab, b, bc),
         TriangleObject2D(ca, bc, c))
      return RecursiveGeoFractalStepResult(outputGeometry, nextGeometry)
   }

   override fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean {
      val tri = geometry as TriangleObject2D
      return listOf(tri.a, tri.b, tri.c).boundingBox().intersectionRect(viewport).isNotEmpty()
   }
}