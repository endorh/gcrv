package endorh.unican.gcrv.renderers

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.renderers.point.CirclePointRenderer
import endorh.unican.gcrv.renderers.spline.VariableInterpolationAntiAliasSplineRenderer
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.transformations.Transform2DStack
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.F
import kotlin.math.roundToInt

interface RenderPass2D {
   val enabled: MutableStateValue<Boolean>
   fun render(canvas: BufferCanvas, objectStack: Object2DStack)
}

interface RenderPassInputScope<Data> {
   fun accept(data: Data)
   fun accept(data: Collection<Data>) {
      for (d in data) accept(d)
   }
   fun accept(vararg data: Data) {
      for (d in data) accept(d)
   }

   fun push(transform: Transform2D)
   fun pop()
}

interface WireframeRenderPassInputScope : RenderPassInputScope<Line2D>
interface PointRenderPassInputScope : RenderPassInputScope<Point2D>
interface CubicSplineRenderPassInputScope : RenderPassInputScope<CubicSpline2D>
interface TaggedColliderPassInputScope<T> : RenderPassInputScope<Pair<Collider, T>>

abstract class RenderPassInputScopeImpl<Data> : RenderPassInputScope<Data> {
   val collected = mutableListOf<Data>()
   val stack = Transform2DStack()

   override fun accept(data: Data) {
      collected.add(transform(data))
   }
   override fun accept(data: Collection<Data>) {
      data.mapTo(collected) { transform(it) }
   }
   override fun accept(vararg data: Data) {
      data.mapTo(collected) { transform(it) }
   }
   abstract fun transform(o: Data): Data
   override fun push(transform: Transform2D) = stack.push(transform)
   override fun pop() = stack.pop()
}
class WireframePassInputScopeImpl : RenderPassInputScopeImpl<Line2D>(), WireframeRenderPassInputScope {
   override fun transform(o: Line2D) = Line2D(
      stack.transform.transform(o.start), stack.transform.transform(o.end), o.style)
}
class PointPassInputScopeImpl : RenderPassInputScopeImpl<Point2D>(), PointRenderPassInputScope {
   override fun transform(o: Point2D) = Point2D(stack.transform.transform(o.pos), o.style)
}
class CubicSplinePassInputScopeImpl : RenderPassInputScopeImpl<CubicSpline2D>(), CubicSplineRenderPassInputScope {
   override fun transform(o: CubicSpline2D) = o.transform(stack.transform)
}
class TaggedColliderPassInputScopeImpl<T> : RenderPassInputScopeImpl<Pair<Collider, T>>(), TaggedColliderPassInputScope<T> {
   override fun transform(o: Pair<Collider, T>) = TransformedCollider(o.first, stack.transform) to o.second
}

data class CubicSplineRenderingSettings(
   val fallbackRenderer: MutableStateValue<CubicSpline2DRenderer> = mutableStateOf(VariableInterpolationAntiAliasSplineRenderer),
   val enforceRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   val fallbackColor: MutableStateValue<Color> = mutableStateOf(Color.WHITE),
   val enforceColor: MutableStateValue<Boolean> = mutableStateOf(false),
   val breadth: MutableStateValue<Float> = mutableStateOf(1F),
   val enforceBreadth: MutableStateValue<Boolean> = mutableStateOf(false),
)

data class WireframeRenderingSettings(
   var fallbackRenderer: MutableStateValue<Line2DRenderer> = mutableStateOf(BresenhamRenderer),
   var enforceRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   var fallbackColor: MutableStateValue<Color> = mutableStateOf(Color.WHITE),
   var enforceColor: MutableStateValue<Boolean> = mutableStateOf(false),
   val breadth: MutableStateValue<Float> = mutableStateOf(1F),
   val enforceBreadth: MutableStateValue<Boolean> = mutableStateOf(false),
)

data class PointRenderingSettings(
   val fallbackRenderer: MutableStateValue<Point2DRenderer> = mutableStateOf(CirclePointRenderer),
   val enforceRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   val fallbackColor: MutableStateValue<Color> = mutableStateOf(Color.WHITE),
   val enforceColor: MutableStateValue<Boolean> = mutableStateOf(false),
   val fallbackSize: MutableStateValue<Int> = mutableStateOf(0),
   val enforceSize: MutableStateValue<Boolean> = mutableStateOf(false),
)

class SplineRenderPass2D(
   val settings: CubicSplineRenderingSettings = CubicSplineRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = CubicSplinePassInputScopeImpl()

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects) render(o)

      // Render primitives
      for (spline in scope.collected) {
         with(
            if (settings.enforceRenderer.value) settings.fallbackRenderer.value
            else spline.style.renderer ?: settings.fallbackRenderer.value
         ) {
            rendererContext.color =
               if (settings.enforceColor.value) settings.fallbackColor.value
               else spline.style.color
            rendererContext.render(spline)
         }
      }
   }
}

class WireframeRenderPass2D(
   val settings: WireframeRenderingSettings = WireframeRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = WireframePassInputScopeImpl()

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects) render(o)

      // Render primitives
      for (line in scope.collected) {
         with(
            if (settings.enforceRenderer.value) settings.fallbackRenderer.value
            else line.style.renderer ?: settings.fallbackRenderer.value
         ) {
            rendererContext.color =
               if (settings.enforceColor.value) settings.fallbackColor.value
               else line.style.color
            rendererContext.render(line)
         }
      }
   }
}

class PointRenderPass2D(
   val settings: PointRenderingSettings = PointRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = PointPassInputScopeImpl()
      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects) render(o)

      // Render primitives
      for (point in scope.collected) {
         with(
            if (settings.enforceRenderer.value) settings.fallbackRenderer.value
            else point.style.renderer ?: settings.fallbackRenderer.value
         ) {
            rendererContext.color =
               if (settings.enforceColor.value) settings.fallbackColor.value
               else point.style.color
            val size = if (settings.enforceSize.value)
               settings.fallbackSize.value else point.style.size.roundToInt()
            val style = point.style.copy(color = rendererContext.color, size = size.F)
            rendererContext.render(point.copy(style = style))
         }
      }
   }
}

class AxesRenderPass2D(var color: Color = Color.GRAY) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      for (x in canvas.origin.x..<canvas.origin.x + canvas.width)
         canvas.F.C[x, 0] = color
      for (y in canvas.origin.y..<canvas.origin.y + canvas.height)
         canvas.F.C[0, y] = color
   }
}

class GridRenderPass2D(var gridSize: Int, var color: Color = Color.DARK_GRAY) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      for (x in canvas.origin.x - canvas.origin.x % gridSize..<canvas.origin.x + canvas.width step gridSize) {
         for (y in canvas.origin.y..<canvas.origin.y + canvas.height)
            canvas.F.C[x, y] = color
      }
      for (y in canvas.origin.y - canvas.origin.y % gridSize..<canvas.origin.y + canvas.height step gridSize) {
         for (x in canvas.origin.x..<canvas.origin.x + canvas.width)
            canvas.F.C[x, y] = color
      }
   }
}

class GizmoRenderPass2D : RenderPass2D {
   override val enabled = mutableStateOf(true)
   var renderedObjects: List<Object2D> = emptyList()

   override fun render(canvas: BufferCanvas, objectStack: Object2DStack) {
      for (o in renderedObjects) renderGizmo(canvas, o)
   }
   fun renderGizmo(canvas: BufferCanvas, obj: Object2D) {
      for (gizmo in obj.gizmos) gizmo.render(canvas)
   }
}

class RenderingPipeline2D(
   var objectStack: Object2DStack, passes: Collection<RenderPass2D> = emptyList(),
   val postPipelines: List<RenderingPipeline2D> = emptyList()
) {
   val renderPasses = passes.toMutableList()

   fun render(canvas: BufferCanvas, update: Boolean = true) {
      canvas.update(update) {
         for (pass in renderPasses)
            if (pass.enabled.value)
               pass.render(canvas, objectStack)
         for (pipeline in postPipelines)
            pipeline.render(canvas, update)
      }
   }

   fun clear(canvas: BufferCanvas, update: Boolean? = null) = canvas.clear(update ?: canvas.autoUpdate)
}