package endorh.unican.gcrv

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.SimpleScene
import de.fabmax.kool.demo.UiSizes
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.PlaybackManager
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.renderers.line.BresenhamRendererBreadth
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.windows.*
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.renderers.spline.VariableInterpolationAntiAliasSplineRenderer
import endorh.unican.gcrv.scene.objects.GroupObject2D
import endorh.unican.gcrv.windows.editor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

class EditorScene : SimpleScene("Line Algorithms"), WindowScene, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    val canvasSize = mutableStateOf(Vec2i(720, 480))
    var canvas = BufferCanvas(canvasSize.value.x, canvasSize.value.y).apply {
        origin.set(Vec2i(-canvasSize.value.x / 2, -canvasSize.value.y / 2))
    }
        private set

    private fun <T> MutableStateValue<T>.affectsCanvas(): MutableStateValue<T> = onChange {
        updateCanvas()
    }

    val canvasUpdates = MutableStateFlow(CanvasUpdateEvent())
    val cubicSplineRenderingSettings = CubicSplineRenderingSettings(
        mutableStateOf<CubicSpline2DRenderer>(VariableInterpolationAntiAliasSplineRenderer).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.WHITE).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(1F).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
    )
    val wireframeSettings = WireframeRenderingSettings(
        mutableStateOf<Line2DRenderer>(BresenhamRendererBreadth).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.WHITE).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(1F).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
    )
    val pointSettings = PointRenderingSettings(
        mutableStateOf<Point2DRenderer>(CircleAntiAliasPointRenderer).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.WHITE).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(5).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
    )
    val timeLine = mutableStateOf(TimeLine())
    val playbackManager = mutableStateOf(PlaybackManager(coroutineContext, timeLine.value))
    val selectedProperties = mutableStateOf<List<AnimProperty<*>>>(emptyList())

    fun loadStack(stack: Object2DStack) {
        objectStack.objects.clear()
        for (o in stack.objects)
            drawObject(o, updateCanvas = false)
        updateCanvas()
    }

    val objectStack = Object2DStack()
    val previewStack = Object2DStack()
    val selectedObjects = mutableStateListOf<Object2D>()

    val brushTypes get() = Object2DTypes.filter { it.objectDrawer != null }
    val brushType = mutableStateOf<Object2DType<*>?>(null)
    val objectDrawingContext = mutableStateOf<SimpleObjectDrawingContext<*>?>(null).onChange {
        previewStack.objects.clear()
        if (it != null) {
            if (selectedObjects.size != 1 || selectedObjects.firstOrNull() !is GroupObject2D) {
                selectedObjects.clear()
                selectedObjects += it.drawnObject
            }
            it.onUpdate = { updateCanvas() }
            previewStack.objects += it.drawnObject
        }
        updateCanvas()
    }

    val gridSize = mutableStateOf(50).affectsCanvas()
    val gridPass = GridRenderPass2D(gridSize.value)
    val axesPass = AxesRenderPass2D()
    val geoSplinePass = SplineRenderPass2D(
        CubicSplineRenderingSettings(
            mutableStateOf(VariableInterpolationAntiAliasSplineRenderer),
            mutableStateOf(true),
            mutableStateOf(Color.GRAY.withAlpha(0.4F)),
            mutableStateOf(true),
            mutableStateOf(1F),
            mutableStateOf(true)
        ), ignoreTransforms = true)
    val geoWireframePass = WireframeRenderPass2D(
        WireframeRenderingSettings(
            mutableStateOf(BresenhamRenderer),
            mutableStateOf(true),
            mutableStateOf(Color.GRAY.withAlpha(0.4F)),
            mutableStateOf(true)
        ), ignoreTransforms = true)
    val geoPointPass = PointRenderPass2D(
        PointRenderingSettings(
            mutableStateOf(CircleAntiAliasPointRenderer),
            mutableStateOf(true),
            mutableStateOf(Color.GRAY.withAlpha(0.4F)),
            mutableStateOf(true),
            mutableStateOf(5),
            mutableStateOf(true),
        ), ignoreTransforms = true)
    val splinePass = SplineRenderPass2D(cubicSplineRenderingSettings)
    val wireframePass = WireframeRenderPass2D(wireframeSettings)
    val pointPass = PointRenderPass2D(pointSettings)
    val gizmoPass = GizmoRenderPass2D().also {
        it.renderedObjects = selectedObjects
    }

    val previewPipeline = RenderingPipeline2D(previewStack, listOf(
        splinePass, wireframePass, pointPass, gizmoPass
    ))
    val pipeline = RenderingPipeline2D(objectStack, listOf(
        gridPass, axesPass,
        geoSplinePass, geoWireframePass, geoPointPass,
        splinePass, wireframePass, pointPass,
        gizmoPass
    ), listOf(previewPipeline))

    init {
       for (p in pipeline.renderPasses)
           p.enabled.affectsCanvas()
    }

    override val selectedColors = mutableStateOf(Colors.darkColors(
        Color("55A4CDFF"),
        Color("789DA6F0"),
        Color("6797A4FF"),
        Color("69797CFF"),
        Color("2B2B2BFF"),
        Color("64646438"),
        Color("FFFFFFFF"),
        Color("FFFFFFFF"),
        Color("D4DBDDFF"),
    )).onChange { dock.dockingSurface.colors = it }
    override val selectedUiSize = mutableStateOf(Sizes.medium)
    override val dock = Dock()
    override val sceneWindows = mutableListOf<BaseWindow<*>>()
    override val windowSpawnLocation = MutableVec2f(320F, 32F)

    // var exampleImage: Texture2d? = null

    init {
       pipeline.render(canvas)
    }

    fun drawObject(obj: Object2D, updateCanvas: Boolean = true, updateSelection: Boolean = true) {
        obj.onPropertyChange { updateCanvas() }
        obj.timeLine.value = timeLine.value
        objectStack.objects += obj
        if (updateSelection) {
            selectedObjects.clear()
            selectedObjects.add(obj)
        }
        if (updateCanvas) updateCanvas()
    }

    fun removeObject(obj: Object2D, updateCanvas: Boolean = true) {
        objectStack.objects -= obj
        selectedObjects.remove(obj)
        if (updateCanvas) updateCanvas()
    }

    fun updateCanvas(clear: Boolean = true) = canvasUpdates.update {
        CanvasUpdateEvent(clear)
    }

    // Must NEVER be equal to other event, otherwise they get conflated
    class CanvasUpdateEvent(val clear: Boolean = true)

    override suspend fun Assets.loadResources(ctx: KoolContext) {
        // exampleImage = loadTexture2d("${SimpleSceneLoader.materialPath}/uv_checker_map.jpg")
    }

    override fun Scene.setupMainScene(ctx: KoolContext) {
        setupUiScene(true)

        dock.dockingSurface.colors = selectedColors.value
        dock.dockingPaneComposable = Composable {
            Box(Grow.Std, Grow.Std) {
                modifier.margin(start = UiSizes.baseSize)
                dock.root()
            }
        }

        addNode(dock)

        dock.createNodeLayout(
            listOf(
                "0:row",
                "0:row/0:col",
                "0:row/0:col/0:leaf",
                "0:row/0:col/1:leaf",
                "0:row/1:col",
                "0:row/1:col/0:leaf",
                "0:row/1:col/1:leaf",
                "0:row/2:col",
                "0:row/2:col/0:leaf",
                "0:row/2:col/1:leaf"
            )
        )

        // Set relative proportions
        dock.getNodeAtPath("0:row/0:col")?.width?.value = Grow(0.25F)
        dock.getNodeAtPath("0:row/1:col")?.width?.value = Grow(1F)
        dock.getNodeAtPath("0:row/2:col")?.width?.value = Grow(0.25F)

        // Add a hidden empty dockable to the center node to avoid, center node being removed on undock
        val centerSpacer = UiDockable("EmptyDockable", dock, isHidden = true)
        dock.getLeafAtPath("0:row/1:col/0:leaf")?.dock(centerSpacer)

        // Spawn initial windows
        val scene = this@EditorScene
        spawnWindow(EditorMenuWindow(scene), "0:row/0:col/0:leaf")
        spawnWindow(ToolWindow(scene), "0:row/0:col/0:leaf")
        spawnWindow(CanvasWindow(scene), "0:row/1:col/0:leaf")
        spawnWindow(RenderSettingsWindow(scene), "0:row/0:col/1:leaf")
        spawnWindow(TransformWindow(scene), "0:row/0:col/1:leaf")
        spawnWindow(GeometryTransformWindow(scene), "0:row/0:col/1:leaf")
        spawnWindow(TimeLineWindow(scene), "0:row/1:col/1:leaf")
        spawnWindow(OutlinerWindow(scene), "0:row/2:col/0:leaf")
        spawnWindow(InspectorWindow(scene), "0:row/2:col/1:leaf")
    }
}
