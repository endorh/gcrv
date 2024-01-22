package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.launchOnMainThread
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.l_system.LSystem
import endorh.unican.gcrv.l_system.LSystemRenderer
import endorh.unican.gcrv.renderers.RenderingPipeline2D
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.Object2DStack
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import endorh.unican.gcrv.windows.BaseWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.measureTimedValue
class LSystemRenderWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("LSystem Canvas", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val maxIterations = 12
   val iterations = mutableStateOf(4).affectsCanvas()
   val maxSeed = 255
   val initialPosition = mutableStateOf(Vec2f.ZERO)
   val initialDirection = mutableStateOf(Vec2f.X_AXIS)

   val viewport = ZoomableViewport(Rect2d(-2.0, -2.0, 2.0, 2.0), false).onChange {
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
      scene.lSystem.onChange { updateCanvas() }
      scene.lSystemRenderer.onChange { updateCanvas() }
   }

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

         Slider(iterations.use().F, 0F, maxIterations.F) {
            modifier.onChange { iterations.value = it.I }.alignY(AlignmentY.Center).margin(start = sizes.largeGap)
         }
         Text("Iter ${iterations.use()}") {
            modifier.margin(start = sizes.smallGap).alignY(AlignmentY.Center)
         }
         Slider(scene.ifsDraft.seed.use(surface).F, 0F, maxSeed.F) {
            modifier.onChange { scene.ifsDraft.seed.value = it.I }.alignY(AlignmentY.Center).margin(start = sizes.largeGap)
         }
         Text("Seed ${scene.ifsDraft.seed.use(surface)}") {
            modifier.margin(start = sizes.smallGap).alignY(AlignmentY.Center)
         }
         LabeledVec2fField("Position", initialPosition) {
            modifier.margin(start = 16.dp).width(80.dp)
         }
         LabeledVec2fField("Direction", initialDirection) {
            modifier.margin(start = sizes.smallGap).width(80.dp)
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
         Button("Z") {
            modifier.alignX(AlignmentX.End).alignY(AlignmentY.Bottom)
               .margin(1.dp).zLayer(10)
               .onClick {
                  viewport.resetZoom()
               }
         }
      }
   }

   fun updateCanvas(onlyTransform: Boolean = false) {
      launch {
         doUpdateCanvas(onlyTransform)
      }
   }

   suspend fun renderLSystem(
      lSystem: LSystem, lSystemRenderer: LSystemRenderer, iterations: Int,
      initialPosition: Vec2f = Vec2f.ZERO, initialDirection: Vec2f = Vec2f.Y_AXIS
   ): List<Object2D> {
      val code = lSystem.generate(iterations)
      // println("LSystem code:\n  $code")
      return lSystemRenderer.render(code, initialPosition, initialDirection).cancellable().toList()
   }

   suspend fun doUpdateCanvas(onlyTransform: Boolean = false) {
      if (!onlyTransform) {
         println("Rendering LSystem")
         val (objects, time) = measureTimedValue {
            renderLSystem(scene.lSystem.value, scene.lSystemRenderer.value, iterations.value, initialPosition.value, initialDirection.value)
         }
         println("LSystem rendered ${objects.size} objects")
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

