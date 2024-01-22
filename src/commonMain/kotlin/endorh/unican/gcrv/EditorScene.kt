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
import endorh.unican.gcrv.animation.TimeRange
import endorh.unican.gcrv.animation.TimeStamp
import endorh.unican.gcrv.scene.*
import endorh.unican.gcrv.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.renderers.line.BresenhamRendererBreadth
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.windows.*
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.renderers.fill.poly.ConvexTestFillRenderer
import endorh.unican.gcrv.renderers.spline.VariableInterpolationSplineRenderer
import endorh.unican.gcrv.scene.objects.GroupObject2D
import endorh.unican.gcrv.serialization.JsonFormat
import endorh.unican.gcrv.util.HttpRequestException
import endorh.unican.gcrv.util.httpGet
import endorh.unican.gcrv.windows.editor.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.coroutines.CoroutineContext

expect object PlatformEditorDefaults {
    val canvasBuffersNum: Int
}

@Serializable
data class ProjectData(
    val objects: Object2DStack,
    val timeRange: TimeRange = TimeRange(TimeStamp(0), TimeStamp(10)),
    val fps: Int = 12
)

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
    val polyFillRenderingSettings = PolyFillRenderingSettings(
        mutableStateOf<PolyFill2DRenderer>(ConvexTestFillRenderer).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.GRAY).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
    )
    val cubicSplineRenderingSettings = CubicSplineRenderingSettings(
        mutableStateOf<CubicSpline2DRenderer>(VariableInterpolationSplineRenderer).affectsCanvas(),
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
    val fps = mutableStateOf(12)
    val playbackManager = mutableStateOf(PlaybackManager(coroutineContext, timeLine.value))
    val selectedProperties = mutableStateOf<List<AnimProperty<*>>>(emptyList())

    fun loadProjectFromJSON(text: String) {
        launch {
            try {
                val projectData = JsonFormat.decodeFromString(serializer<ProjectData>(), text)
                loadProjectData(projectData)
            } catch (e: Exception) {
                println("Failed to load project: $e")
                e.printStackTrace()
                throw e
            }
        }
    }

    fun saveProjectToJSON(): String {
        return JsonFormat.encodeToString(serializer<ProjectData>(), saveProjectData())
    }

    fun loadProjectData(data: ProjectData) {
        timeLine.value.renderedRange = data.timeRange.copy()
        fps.value = data.fps
        loadStack(data.objects)
    }

    fun saveProjectData() = ProjectData(objectStack, timeLine.value.renderedRange.copy(), fps.value)

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
            mutableStateOf(VariableInterpolationSplineRenderer),
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
    val polyFillPass = PolyFillRenderPass2D(polyFillRenderingSettings)
    val splinePass = SplineRenderPass2D(cubicSplineRenderingSettings)
    val wireframePass = WireframeRenderPass2D(wireframeSettings)
    val pointPass = PointRenderPass2D(pointSettings)
    val gizmoPass = GizmoRenderPass2D(false, selectedObjects)

    val previewPipeline = RenderingPipeline2D(previewStack, listOf(
        splinePass, wireframePass, pointPass, gizmoPass
    ))
    val pipeline = RenderingPipeline2D(objectStack, listOf(
        gridPass, axesPass,
        geoSplinePass, geoWireframePass, geoPointPass,
        polyFillPass,
        splinePass, wireframePass, pointPass,
        gizmoPass
    ), listOf(previewPipeline))

    val canvasBuffersNum = mutableStateOf(PlatformEditorDefaults.canvasBuffersNum)

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

    override fun loadStartupParams(params: Map<String, String>) {
        params["project"]?.let {
            loadDemoProject(it)
        }
        if ("autoplay" in params) launch {
            delay(100L)
            playbackManager.value.play()
        }
    }

    fun loadDemoProject(project: String) {
        println("Loading project: $project")
        if (project.startsWith("http://") || project.startsWith("https://")) {
            launch {
                val data = try {
                    httpGet(project)
                } catch (e: HttpRequestException) {
                    println("Failed to load project: ${e.message}")
                    return@launch
                }

                try {
                    loadProjectFromJSON(data)
                } catch (e: SerializationException) {
                    println("Failed to load project, invalid project file format: ${e.message}")
                    e.printStackTrace()
                    return@launch
                }

                println("Loaded project from params")
            }
        } else {

        }
        // Assets.loadBlob("demos/$project.json")
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
