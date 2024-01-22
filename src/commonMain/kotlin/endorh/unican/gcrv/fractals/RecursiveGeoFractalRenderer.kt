package endorh.unican.gcrv.fractals

import endorh.unican.gcrv.fractals.renderers.*
import endorh.unican.gcrv.renderers.PresentableObject
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.util.Rect2f
import kotlinx.coroutines.yield

data class RecursiveGeoFractalStepResult(
   val outputGeometry: List<Object2D>,
   val nextGeometry: List<Object2D>,
)

interface RecursiveGeoFractalRenderer : PresentableObject {
   val initialGeometry: List<Object2D>
   val extraGeometry: List<Object2D> get() = emptyList()
   val finalStepAsOutput: Boolean get() = true
   fun step(geometry: Object2D, step: Int, subStep: Int): RecursiveGeoFractalStepResult
   fun filter(geometry: Object2D, step: Int, subStep: Int, viewport: Rect2f): Boolean = true
}

suspend fun renderRecursiveFractal(
   renderer: RecursiveGeoFractalRenderer, steps: Int, viewport: Rect2f? = null
): List<Object2D> {
   var pending = renderer.initialGeometry
   val output = renderer.extraGeometry.toMutableList()
   for (step in 0 until steps) {
      yield()
      val next = mutableListOf<Object2D>()
      for ((subStep, geo) in pending.withIndex()) {
         yield()
         if (viewport == null || renderer.filter(geo, step, subStep, viewport)) {
            val (outputGeometry, nextGeometry) = renderer.step(geo, step, subStep)
            output.addAll(outputGeometry)
            next.addAll(nextGeometry)
         }
      }
      pending = next
   }
   if (renderer.finalStepAsOutput) for ((subStep, geo) in pending.withIndex()) {
      yield()
      if (viewport == null || renderer.filter(geo, steps, subStep, viewport)) output.add(geo)
   }
   return output
}

val RecursiveGeoFractalRenderers: List<RecursiveGeoFractalRenderer> = listOf(
   SierpinskyTriangleRenderer,
   KochCurveRenderer,
   SierpinskyCarpetRenderer,
   DragonNailRenderer,
   HilbertCurveRenderer,
)