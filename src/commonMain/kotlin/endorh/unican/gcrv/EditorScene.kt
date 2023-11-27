package endorh.unican.gcrv

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.SimpleScene
import de.fabmax.kool.demo.SimpleSceneLoader
import de.fabmax.kool.demo.UiSizes
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.launchDelayed
import endorh.unican.gcrv.animation.PlaybackManager
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.line_algorithms.*
import endorh.unican.gcrv.line_algorithms.renderers.line.BresenhamRenderer
import endorh.unican.gcrv.line_algorithms.renderers.line.BresenhamRendererBreadth
import endorh.unican.gcrv.line_algorithms.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.windows.*
import endorh.unican.gcrv.objects.AnimProperty
import endorh.unican.gcrv.objects.Object2D
import endorh.unican.gcrv.objects.Object2DStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

class EditorScene : SimpleScene("Line Algorithms"), CoroutineScope {
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
    val toolLineStyle = mutableStateOf(LineStyle(Color.WHITE))
    val wireframeSettings = WireframeRenderingSettings(
        mutableStateOf<Line2DRenderer>(BresenhamRendererBreadth).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.WHITE).affectsCanvas(),
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

    val objectStack = Object2DStack()
    val selectedObjects = mutableStateListOf<Object2D>()

    val axesPass = AxesRenderPass2D()
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
    val wireframePass = WireframeRenderPass2D(wireframeSettings)
    val pointPass = PointRenderPass2D(pointSettings)

    val pipeline: RenderingPipeline2D = RenderingPipeline2D(objectStack, listOf(
        axesPass,
        geoWireframePass, geoPointPass,
        wireframePass, pointPass
    ))

    init {
       for (p in pipeline.renderPasses)
           p.enabled.affectsCanvas()
    }

    val selectedColors = mutableStateOf(Colors.darkColors(
        Color("55A4CDFF"),
        Color("789DA6F0"),
        Color("6797A4FF"),
        Color("69797CFF"),
        Color("2B2B2BFF"),
        Color("80808042"),
        Color("FFFFFFFF"),
        Color("FFFFFFFF"),
        Color("D4DBDDFF"),
    )).onChange { dock.dockingSurface.colors = it }
    val selectedUiSize = mutableStateOf(Sizes.medium)

    val dock = Dock()
    val subWindows = mutableListOf<BaseWindow>()

    private val windowSpawnLocation = MutableVec2f(320F, 64F)

    var exampleImage: Texture2d? = null

    init {
       pipeline.render(canvas)
    }

    fun drawObject(obj: Object2D, update: Boolean = true) {
        obj.onPropertyChange { updateCanvas() }
        obj.properties.allProperties.forEach {
            it.timeLine.value = timeLine.value
        }
        objectStack.objects += obj
        if (update) updateCanvas()
    }

    fun removeObject(obj: Object2D, update: Boolean = true) {
        objectStack.objects -= obj
        if (update) updateCanvas()
    }

    fun updateCanvas(clear: Boolean = true) = canvasUpdates.update { CanvasUpdateEvent(clear) }

    class CanvasUpdateEvent(val clear: Boolean = true)

    override suspend fun Assets.loadResources(ctx: KoolContext) {
        exampleImage = loadTexture2d("${SimpleSceneLoader.materialPath}/uv_checker_map.jpg")
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
        spawnWindow(MenuWindow(scene), "0:row/0:col/0:leaf")
        spawnWindow(CanvasWindow(scene), "0:row/1:col/0:leaf")
        spawnWindow(RenderSettingsWindow(scene), "0:row/0:col/1:leaf")
        spawnWindow(ToolWindow(scene), "0:row/0:col/0:leaf")
        spawnWindow(TransformWindow(scene), "0:row/0:col/0:leaf")
        spawnWindow(TimeLineWindow(scene), "0:row/1:col/1:leaf")
        spawnWindow(OutlinerWindow(scene), "0:row/2:col/0:leaf")
        spawnWindow(InspectorWindow(scene), "0:row/2:col/1:leaf")
    }

    fun spawnWindow(window: BaseWindow, dockPath: String? = null) {
        subWindows += window

        dock.addDockableSurface(window.windowDockable, window.windowSurface)
        dockPath?.let { dock.getLeafAtPath(it)?.dock(window.windowDockable) }

        window.windowDockable.setFloatingBounds(Dp(windowSpawnLocation.x), Dp(windowSpawnLocation.y))
        windowSpawnLocation.x += 32F
        windowSpawnLocation.y += 32F

        if (windowSpawnLocation.y > 480F) {
            windowSpawnLocation.y -= 416
            windowSpawnLocation.x -= 384
            if (windowSpawnLocation.x > 480F)
                windowSpawnLocation.x = 320F
        }

        launchDelayed(1) {
            window.windowSurface.isFocused.set(true)
        }
    }

    fun closeWindow(window: BaseWindow, ctx: KoolContext) {
        dock.removeDockableSurface(window.windowSurface)
        subWindows -= window
        window.onClose()
        window.windowSurface.dispose(ctx)
    }
}
