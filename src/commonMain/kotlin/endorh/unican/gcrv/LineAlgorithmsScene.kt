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
import endorh.unican.gcrv.line_algorithms.*
import endorh.unican.gcrv.line_algorithms.renderers.line.BresenhamRendererBreadth
import endorh.unican.gcrv.line_algorithms.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.line_algorithms.renderers.point.CirclePointRenderer
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.windows.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

class LineAlgorithmsScene : SimpleScene("Line Algorithms"), CoroutineScope {
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
        mutableStateOf<Point2DRenderer>(CircleAntiAliasPointRenderer).affectsCanvas(),
        mutableStateOf(false).affectsCanvas(),
        mutableStateOf(Color.RED).affectsCanvas(),
        mutableStateOf(Color.BLUE).affectsCanvas(),
        mutableStateOf(true).affectsCanvas(),
        mutableStateOf(5).affectsCanvas(),
    )

    val objectStack = Object2DStack()
    val selectedObjects = mutableStateListOf<Object2D>()
    val pipeline: RenderingPipeline2D = RenderingPipeline2D(objectStack, listOf(
        AxesRenderPass2D(),
        WireframeRenderPass2D(wireframeSettings),
    ))

    val selectedColors = mutableStateOf(Colors.darkColors()).onChange { dock.dockingSurface.colors = it }
    val selectedUiSize = mutableStateOf(Sizes.medium)

    val dock = Dock()
    val subWindows = mutableListOf<BaseWindow>()

    private val windowSpawnLocation = MutableVec2f(320f, 64f)

    var exampleImage: Texture2d? = null

    init {
       pipeline.render(canvas)
    }

    fun drawObject(obj: Object2D, update: Boolean = true) {
        obj.onPropertyChange { updateCanvas() }
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
                "0:row/0:leaf",
                "0:row/1:col",
                "0:row/1:col/0:leaf",
                "0:row/1:col/1:row",
                "0:row/1:col/1:row/0:leaf",
                "0:row/1:col/1:row/1:leaf",
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
        val scene = this@LineAlgorithmsScene
        spawnWindow(MenuWindow(scene), "0:row/0:leaf")
        spawnWindow(LineCanvasWindow(scene), "0:row/1:col/0:leaf")
        spawnWindow(RenderSettingsWindow(scene), "0:row/1:col/1:row/0:leaf")
        spawnWindow(ToolWindow(scene), "0:row/1:col/1:row/1:leaf")
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
