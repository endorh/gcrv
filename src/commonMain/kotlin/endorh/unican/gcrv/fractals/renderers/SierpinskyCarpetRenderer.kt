package endorh.unican.gcrv.fractals.renderers

import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalStepResult
import endorh.unican.gcrv.renderers.fill.poly.NoOpFillRenderer
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.PolyFillStyle
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.scene.objects.PolygonObject2D
import endorh.unican.gcrv.serialization.Color
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.ui2.TRANSPARENT
import endorh.unican.gcrv.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable @SerialName("sierpinsky-carpet")
object SierpinskyCarpetRenderer : RecursiveGeoFractalRenderer {
   private val SQRT_3 = sqrt(3F)
   private val SQRT_3_6 = SQRT_3 / 6F
   private val polygonStyle = PolyFillStyle(Color.TRANSPARENT, NoOpFillRenderer)

   override val displayName get() = "Sierpinsky Carpet"
   override val initialGeometry = listOf(
      LineObject2D(Vec2f(-0.5F, -0.5F), Vec2f(0.5F, 0.5F)))
   override val extraGeometry = listOf(PolygonObject2D(listOf(
      Vec2f(-0.5F, -0.5F), Vec2f(-0.5F, 0.5F),
      Vec2f(0.5F, 0.5F), Vec2f(0.5F, -0.5F)), fillStyle=polygonStyle))
   override val finalStepAsOutput: Boolean get() = false

   override fun step(geometry: Object2D, step: Int, subStep: Int): RecursiveGeoFractalStepResult {
      val line = geometry as LineObject2D
      val minX = line.start.x
      val maxX = line.end.x
      val minY = line.start.y
      val maxY = line.end.y
      val x13 = (2F * minX + maxX) / 3F
      val x23 = (minX + 2F * maxX) / 3F
      val y13 = (2F * minY + maxY) / 3F
      val y23 = (minY + 2F * maxY) / 3F
      val output = listOf(
         PolygonObject2D(listOf(
            Vec2f(x13, y13), Vec2f(x23, y13),
            Vec2f(x23, y23), Vec2f(x13, y23)), fillStyle=polygonStyle))
      val nextGeometry = listOf(
         LineObject2D(Vec2f(minX, minY), Vec2f(x13, y13)),
         LineObject2D(Vec2f(x13, minY), Vec2f(x23, y13)),
         LineObject2D(Vec2f(x23, minY), Vec2f(maxX, y13)),
         LineObject2D(Vec2f(minX, y13), Vec2f(x13, y23)),
         LineObject2D(Vec2f(x23, y13), Vec2f(maxX, y23)),
         LineObject2D(Vec2f(minX, y23), Vec2f(x13, maxY)),
         LineObject2D(Vec2f(x13, y23), Vec2f(x23, maxY)),
         LineObject2D(Vec2f(x23, y23), Vec2f(maxX, maxY)))
      return RecursiveGeoFractalStepResult(output, nextGeometry)
   }

   override fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean {
      val line = geometry as LineObject2D
      return Rect2f(line.start.x, line.start.y, line.end.x, line.end.y).intersectionRect(viewport).isNotEmpty()
   }
}