package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.launchOnMainThread
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderers
import endorh.unican.gcrv.fractals.renderRecursiveFractal
import endorh.unican.gcrv.renderers.OptionPicker
import endorh.unican.gcrv.renderers.RenderingPipeline2D
import endorh.unican.gcrv.scene.Object2DStack
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import endorh.unican.gcrv.windows.BaseWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class RecursiveFractalsWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("Recursive Fractals", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val maxIterations = 16
   val iterations = mutableStateOf(4).affectsCanvas()
   val renderer = mutableStateOf<RecursiveGeoFractalRenderer>(RecursiveGeoFractalRenderers.first()).affectsCanvas() // REPORT: When RecursiveGeoFractalRenderers does not have an explicit type, type inference fails here only when compiling to JS
   val selectiveGeneration = mutableStateOf(false).affectsCanvas()

   val viewport = ZoomableViewport().onChange {
      canvasUpdates.update { CanvasUpdateEvent(onlyTransform=true) }
   }
   val canvas = ResizableBufferCanvas().onResize {
      canvasUpdates.update { CanvasUpdateEvent() }
   }
   val canvasUpdates = MutableStateFlow(CanvasUpdateEvent())
   private var lastRenderTime = mutableStateOf(Duration.ZERO)
   private var updateJob = mutableStateOf<Job?>(null)
   private var updatePublicationJob: Job? = null
   val updateCount = mutableStateOf(0)

   init {
      launch {
         canvasUpdates.collectLatest {
            updateJob.value?.cancelAndJoin()
            updatePublicationJob?.join()
            updatePublicationJob = null
            updateJob.value = launch {
               doUpdateCanvas(it.onlyTransform)
            }
         }
      }
   }

   // Must NEVER be equal to other event, otherwise they get conflated
   class CanvasUpdateEvent(val onlyTransform: Boolean = false)

   val objectStack = Object2DStack()
   val pipeline: RenderingPipeline2D = RenderingPipeline2D.basicPipeline(objectStack)

   private fun <T> MutableStateValue<T>.affectsCanvas() = onChange {
      canvasUpdates.update { CanvasUpdateEvent() }
   }

   override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
      modifier.padding(horizontal = sizes.smallGap, vertical = sizes.smallGap)

      Row {
         modifier.margin(sizes.smallGap).height(28.dp)

         Text("Fractal: ") {
            modifier.margin(end = sizes.smallGap).alignY(AlignmentY.Center)
         }
         OptionPicker(RecursiveGeoFractalRenderers, renderer.use(), { renderer.value = it }) {
            modifier.margin(end = sizes.smallGap).alignY(AlignmentY.Center)
         }

         Text("Iterations: ${iterations.use()}") {
            modifier.margin(start = sizes.smallGap).alignY(AlignmentY.Center)
         }
         Slider(iterations.use().F, 0F, maxIterations.F) {
            modifier.onChange { iterations.value = it.I }.alignY(AlignmentY.Center)
         }
         Text("Selective rendering: ") {
            modifier.margin(start = sizes.smallGap).alignY(AlignmentY.Center)
         }
         Checkbox(selectiveGeneration.use()) {
            modifier.margin(start = sizes.smallGap).alignY(AlignmentY.Center).onToggle {
               selectiveGeneration.value = it
            }
         }
      }

      Box {
         modifier.size(Grow.Std, Grow.Std)
            .viewportControls(viewport)
         ResizableCanvas(canvas) {
            modifier.margin(4.dp)
            updateCount.use()
         }
         Text("${lastRenderTime.use().inWholeMilliseconds} ms${if (updateJob.use() != null) " ~ " else ""}") {
            modifier.alignX(AlignmentX.Start).alignY(AlignmentY.Bottom)
               .padding(1.dp).zLayer(10)
               .textColor(Color.GRAY)
         }
      }
   }

   fun updateCanvas(onlyTransform: Boolean = false) {
      launch {
         doUpdateCanvas(onlyTransform)
      }
   }

   suspend fun doUpdateCanvas(onlyTransform: Boolean = false) {
      if (!onlyTransform) {
         val (objects, time) = measureTimedValue {
            renderRecursiveFractal(
               renderer.value, iterations.value,
               if (selectiveGeneration.value) viewport.displayedRect.toRect2f() else null)
         }
         lastRenderTime.value = time
         objectStack.objects.clear()
         objectStack.objects += objects
      }

      pipeline.transform = viewport.canvasTransform(canvas.size)
      pipeline.clear(canvas.canvas, false)
      pipeline.render(canvas.canvas, false)

      // Uploading a texture to GPU needs to happen on the render thread
      updatePublicationJob = launchOnMainThread {
         canvas.canvas.update()
      }
      updateJob.value = null
   }
}