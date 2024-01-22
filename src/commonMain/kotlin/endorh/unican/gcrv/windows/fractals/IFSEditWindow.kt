package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.launchOnMainThread
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.fractals.IFSFunctionDraft
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.scene.Object2DStack
import endorh.unican.gcrv.scene.objects.IFSGizmoObject2D
import endorh.unican.gcrv.transformations.TaggedTransform2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.Rect2d
import endorh.unican.gcrv.windows.BaseWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

class IFSEditWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("IFS Editor", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val viewport = ZoomableViewport(
      Rect2d(-2.0, -2.0, 2.0, 2.0), false
   ).onChange {
      updateCanvas()
   }
   val canvas = ResizableBufferCanvas().onResize {
      updateCanvas()
   }
   val canvasUpdates = MutableStateFlow(CanvasUpdateEvent())
   fun updateCanvas() {
      canvasUpdates.update { CanvasUpdateEvent() }
   }

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
               doUpdateCanvas()
            }
         }
      }
   }

   // Must NEVER be equal to other event, otherwise they get conflated
   class CanvasUpdateEvent()

   val draggingGizmos = mutableStateOf(false)
   val selectedObjects = mutableListOf<IFSGizmoObject2D>()
   val focusedObject = mutableStateOf<IFSGizmoObject2D?>(null)

   val objectStack = Object2DStack()
   val pipeline: RenderingPipeline2D = RenderingPipeline2D(objectStack, listOf(
      PolyFillRenderPass2D(),
      SplineRenderPass2D(),
      WireframeRenderPass2D(),
      PointRenderPass2D(),
      GizmoRenderPass2D(renderedObjects=selectedObjects)
   ))

   fun updateGizmos() {
      objectStack.objects.removeAll { obj ->
         obj is IFSGizmoObject2D && scene.ifsDraft.functions.value.none { it === obj.draft }
      }
      scene.ifsDraft.functions.value.forEach { draft ->
         if (objectStack.objects.none { (it as? IFSGizmoObject2D)?.draft === draft })
            objectStack.objects.add(IFSGizmoObject2D(draft).apply {
               onPropertyChange {
                  updateCanvas()
               }
            })
      }
      selectedObjects.clear()
      selectedObjects.addAll(objectStack.objects.filterIsInstance<IFSGizmoObject2D>())
      if (focusedObject.value?.draft?.let { draft -> scene.ifsDraft.functions.value.none { it === draft }} != false) {
         focusedObject.value = selectedObjects.firstOrNull()
      }
   }

   init {
      // objectStack.objects += PolygonObject2D(listOf(
      //    Vec2f(-1F, -1F), Vec2f(1F, -1F), Vec2f(1F, 1F), Vec2f(-1F, 1F)),
      //    LineStyle(Color.GRAY, 2F, BresenhamRendererBreadthAntiAlias),
      //    PointStyle(Color.GRAY, 0F),
      //    PolyFillStyle(Color.TRANSPARENT))
      // objectStack.objects += LineObject2D(
      //    Vec2f(-1F, 0F), Vec2f(1F, 0F),
      //    LineStyle(Color.GRAY.withAlpha(0.5F), 1F, BresenhamRendererBreadthAntiAlias),
      //    PointStyle(Color.GRAY, 0F))
      // objectStack.objects += LineObject2D(
      //    Vec2f(0F, -1F), Vec2f(0F, 1F),
      //    LineStyle(Color.GRAY.withAlpha(0.5F), 1F, BresenhamRendererBreadthAntiAlias),
      //    PointStyle(Color.GRAY, 0F))

      scene.ifsDraft.onChange {
         if (!draggingGizmos.value) updateGizmos()
         updateCanvas()
      }
      updateGizmos()
      updateCanvas()
   }


   override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
      modifier.padding(horizontal = sizes.smallGap, vertical = sizes.smallGap)

      Row(Grow.Std) {
         modifier.backgroundColor(colors.backgroundVariant).height(28.dp)
         Button("+") {
            modifier.height(28.dp)
            onClick {
               scene.ifsDraft.functions.value += IFSFunctionDraft(TaggedTransform2D(scale=Vec2f(0.5F, 0.5F)))
               updateGizmos()
               updateCanvas()
            }
         }
         Button("-") {
            modifier.margin(start=4.dp).height(28.dp)
            onClick {
               focusedObject.value?.let { focused ->
                  scene.ifsDraft.functions.value = scene.ifsDraft.functions.value.filter { it !== focused.draft }
               } ?: run {
                  scene.ifsDraft.functions.value = scene.ifsDraft.functions.value.dropLast(1)
               }
               updateGizmos()
               updateCanvas()
            }
         }

         focusedObject.use()?.let { o ->
            ColorField(o.draft.color.use(surface), { focusedObject.value?.draft?.color?.value = it }) {
               modifier.margin(start=8.dp).width(100.dp).height(28.dp)
            }
            LabeledFloatField("Weight", o.draft.weight.use(surface), { o.draft.weight.value = it }) {
               modifier.margin(start=4.dp).width(100.dp).height(28.dp)
            }
         }
      }

      Box {
         modifier.size(Grow.Std, Grow.Std)
         ResizableCanvas(canvas) {
            modifier.margin(4.dp)
               .viewportControls(viewport) { !it.pointer.isLeftButtonDown }
            pickingControls(objectStack, selectedObjects, viewport) {
               focusedObject.value = it as? IFSGizmoObject2D
            }
            gizmoControls(objectStack, selectedObjects, viewport, {
               draggingGizmos.value = it
            })
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

   suspend fun doUpdateCanvas() {
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