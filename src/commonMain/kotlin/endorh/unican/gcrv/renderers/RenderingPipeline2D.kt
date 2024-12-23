package endorh.unican.gcrv.renderers

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.fill.poly.ConvexTestFillRenderer
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.renderers.point.CirclePointRenderer
import endorh.unican.gcrv.renderers.spline.VariableInterpolationSplineRenderer
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.transformations.Transform2DStack
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.toVec2i
import kotlin.math.roundToInt

interface RenderPass2D {
   val enabled: MutableStateValue<Boolean>
   fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D? = null)
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

interface RenderPassIntInputScope<Data, IntData> : RenderPassInputScope<Data> {
   fun acceptInt(data: IntData)
   fun acceptInt(data: Collection<IntData>)
   fun acceptInt(vararg data: IntData)
}
interface WireframeRenderPassInputScope : RenderPassIntInputScope<LineSegment2f, LineSegment2i>
interface PointRenderPassInputScope : RenderPassIntInputScope<Point2f, Point2i>
interface CubicSplineRenderPassInputScope : RenderPassInputScope<CubicSpline2f>
interface PolyFillRenderPassInputScope : RenderPassInputScope<PolyFill2f>
interface TaggedColliderPassInputScope<T> : RenderPassInputScope<Pair<Collider2D, T>>
interface TransformedGizmoPassInputScope : RenderPassInputScope<Gizmo2D>

abstract class RenderPassInputScopeImpl<Data, Transformed> : RenderPassInputScope<Data> {
   val collected = mutableListOf<Transformed>()
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
   abstract fun transform(o: Data): Transformed
   override fun push(transform: Transform2D) = stack.push(transform)
   override fun pop() = stack.pop()
}
abstract class RenderPassIntInputScopeImpl<Data, IntData, Transformed> : RenderPassInputScopeImpl<Data, Transformed>(), RenderPassIntInputScope<Data, IntData> {
   abstract fun transformInt(o: IntData): Transformed
   override fun acceptInt(data: IntData) {
      collected.add(transformInt(data))
   }
   override fun acceptInt(data: Collection<IntData>) {
      data.mapTo(collected) { transformInt(it) }
   }
   override fun acceptInt(vararg data: IntData) {
      data.mapTo(collected) { transformInt(it) }
   }
}
class WireframePassInputScopeImpl : RenderPassIntInputScopeImpl<LineSegment2f, LineSegment2i, LineSegment2i>(), WireframeRenderPassInputScope {
   override fun transform(o: LineSegment2f) = LineSegment2i(
      stack.transform.transform(o.start).toVec2i(), stack.transform.transform(o.end).toVec2i(), o.style)
   override fun transformInt(o: LineSegment2i) = LineSegment2i(
      stack.transform.transform(o.start), stack.transform.transform(o.end), o.style)
}
class PointPassInputScopeImpl : RenderPassIntInputScopeImpl<Point2f, Point2i, Point2i>(), PointRenderPassInputScope {
   override fun transform(o: Point2f) = Point2i(stack.transform.transform(o.pos).toVec2i(), o.style)
   override fun transformInt(o: Point2i) = Point2i(stack.transform.transform(o.pos), o.style)
}
class CubicSplinePassInputScopeImpl : RenderPassInputScopeImpl<CubicSpline2f, CubicSpline2f>(), CubicSplineRenderPassInputScope {
   override fun transform(o: CubicSpline2f) = o.transform(stack.transform)
}
class PolyFillPassInputScopeImpl : RenderPassInputScopeImpl<PolyFill2f, PolyFill2f>(), PolyFillRenderPassInputScope {
   override fun transform(o: PolyFill2f) = PolyFill2f(o.points.map { stack.transform * it }, o.style)
}
class TaggedColliderPassInputScopeImpl<T> : RenderPassInputScopeImpl<Pair<Collider2D, T>, Pair<TransformedCollider2D, T>>(), TaggedColliderPassInputScope<T> {
   override fun transform(o: Pair<Collider2D, T>) = TransformedCollider2D(o.first, stack.transform) to o.second
}
class TransformedGizmoPassInputScopeImpl : RenderPassInputScopeImpl<Gizmo2D, TransformedGizmo>(), TransformedGizmoPassInputScope {
   override fun transform(o: Gizmo2D) = TransformedGizmo(o, stack.transform)
}

data class PolyFillRenderingSettings(
   val fallbackRenderer: MutableStateValue<PolyFill2DRenderer> = mutableStateOf(ConvexTestFillRenderer),
   val enforceRenderer: MutableStateValue<Boolean> = mutableStateOf(false),
   val fallbackColor: MutableStateValue<Color> = mutableStateOf(Color.WHITE),
   val enforceColor: MutableStateValue<Boolean> = mutableStateOf(false)
)

data class CubicSplineRenderingSettings(
   val fallbackRenderer: MutableStateValue<CubicSpline2DRenderer> = mutableStateOf(VariableInterpolationSplineRenderer),
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

class PolyFillRenderPass2D(
   val settings: PolyFillRenderingSettings = PolyFillRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = PolyFillPassInputScopeImpl()
      transform?.let { scope.push(it) }

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with (renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      for (o in objectStack.objects.snapshot) render(o)

      for (fill in scope.collected) with(
         if (settings.enforceRenderer.value) settings.fallbackRenderer.value
         else fill.style.renderer ?: settings.fallbackRenderer.value
      ) {
         rendererContext.color =
            if (settings.enforceColor.value) settings.fallbackColor.value
            else fill.style.color
         rendererContext.render(fill)
      }
   }
}

class SplineRenderPass2D(
   val settings: CubicSplineRenderingSettings = CubicSplineRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = CubicSplinePassInputScopeImpl()
      transform?.let { scope.push(it) }

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects.snapshot) render(o)

      // Render primitives
      for (spline in scope.collected) with(
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

class WireframeRenderPass2D(
   val settings: WireframeRenderingSettings = WireframeRenderingSettings(),
   val ignoreTransforms: Boolean = false
) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = WireframePassInputScopeImpl()
      transform?.let { scope.push(it) }

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects.snapshot) render(o)

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
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      val rendererContext = CanvasPixelRendererContext(canvas)
      val scope = PointPassInputScopeImpl()
      transform?.let { scope.push(it) }

      fun render(o: Object2D) {
         if (!ignoreTransforms) scope.push(o.aggregatedTransform)
         for (renderer in o.renderers) with(renderer) {
            scope.render()
         }
         for (child in o.children) render(child)
         if (!ignoreTransforms) scope.pop()
      }

      // Collect primitives
      for (o in objectStack.objects.snapshot) render(o)

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
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      for (x in canvas.origin.x..<canvas.origin.x + canvas.width)
         canvas.F.C[x, 0] = color
      for (y in canvas.origin.y..<canvas.origin.y + canvas.height)
         canvas.F.C[0, y] = color
   }
}

class GridRenderPass2D(var gridSize: Int, var color: Color = Color.DARK_GRAY) : RenderPass2D {
   override val enabled = mutableStateOf(true)
   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
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

class GizmoRenderPass2D(val ignoreTransforms: Boolean = false, var renderedObjects: Collection<Object2D>? = null) : RenderPass2D {
   override val enabled = mutableStateOf(true)

   override fun render(canvas: BufferCanvas, objectStack: Object2DStack, transform: Transform2D?) {
      objectStack.collectGizmos(renderedObjects, ignoreTransforms, transform).forEach { it.render(canvas) }
   }
}

class RenderingPipeline2D(
   var objectStack: Object2DStack, passes: Collection<RenderPass2D> = emptyList(),
   val postPipelines: List<RenderingPipeline2D> = emptyList()
) {
   var transform: Transform2D? = null
   val renderPasses = passes.toMutableList()

   fun render(canvas: BufferCanvas, update: Boolean = true) {
      canvas.update(update) {
         for (pass in renderPasses)
            if (pass.enabled.value)
               pass.render(canvas, objectStack, transform)
         for (pipeline in postPipelines)
            pipeline.render(canvas, update)
      }
   }

   fun clear(canvas: BufferCanvas, update: Boolean? = null) = canvas.clear(update ?: canvas.autoUpdate)

   companion object {
      fun basicPipeline(objectStack: Object2DStack, renderedObjects: Collection<Object2D>? = emptySet()) =
         RenderingPipeline2D(objectStack, listOf(
            PolyFillRenderPass2D(),
            SplineRenderPass2D(),
            WireframeRenderPass2D(),
            PointRenderPass2D(),
            GizmoRenderPass2D(false, renderedObjects)))
   }
}