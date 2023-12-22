@file:OptIn(ExperimentalUnsignedTypes::class)

package endorh.unican.gcrv.windows

import de.fabmax.kool.math.MutableVec2i
import de.fabmax.kool.math.randomF
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.RenderLoop
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.WindowScene
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.ui2.Canvas
import endorh.unican.gcrv.util.B
import endorh.unican.gcrv.util.ByteArray2D
import endorh.unican.gcrv.util.IntArray2D
import endorh.unican.gcrv.util.RGBA
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.min

class GameOfLifeCanvasWindow(scene: WindowScene) : BaseWindow<WindowScene>("Conway`s Game of Life", scene), CoroutineScope {
    override val coroutineContext = EmptyCoroutineContext + Dispatchers.Default

    private val world = GameWorld(coroutineContext)
    private val canvasRenderer = PixelCanvasRenderer()

    private val isPauseOnEdit = mutableStateOf(false)

    private val canvasPanel = CanvasPanel()
    private val renderPanel = RenderPanel()

    init {
        world.launch {
            world.loadAsciiState(GameWorld.gliderGun)
        }
    }

    override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
        canvasPanel()
        renderPanel()

        ScrollArea(
            containerModifier = {
                it
                    .margin(sizes.gap)
                    .size(Grow(1F, max = FitContent), Grow(1F, max = FitContent))
                    .backgroundColor(if (canvasRenderer.isClassicColor) colors.backgroundVariant else MdColor.GREY tone 900)
            }
        ) {
            canvasRenderer()
        }
    }

    private abstract class CollapsablePanel(val title: String) : Composable {
        val isCollapsed = mutableStateOf(false)
        val isHovered = mutableStateOf(false)

        override fun UiScope.compose() = Column(Grow.Std) {
            modifier.backgroundColor(colors.backgroundVariant)
            Row(Grow.Std) {
                modifier
                    .backgroundColor(colors.secondaryVariantAlpha(if (isHovered.use()) 0.75F else 0.5F))
                    .onClick { isCollapsed.toggle() }
                    .padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                    .onEnter { isHovered.set(true) }
                    .onExit { isHovered.set(false) }

                Arrow (if (isCollapsed.use()) 0F else 90F) {
                    modifier
                        .size(sizes.gap * 1.5F, sizes.gap * 1.5F)
                        .margin(horizontal = sizes.gap)
                        .alignY(AlignmentY.Center)
                }
                Text(title) { }
            }
            if (!isCollapsed.value) {
                content()
            } else {
                divider(colors.secondaryVariantAlpha(0.75F), horizontalMargin = 0.dp, thickness = 1.dp)
            }
        }

        abstract fun UiScope.content()
    }

    private inner class CanvasPanel : CollapsablePanel("Canvas") {
        override fun UiScope.content() {
            Row {
                modifier.padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                Text("Content") { modifier.alignY(AlignmentY.Center).width(sizes.largeGap * 7F) }
                Button("Clear world") {
                    modifier
                        .width(sizes.largeGap * 7F)
                        .margin(horizontal = sizes.gap)
                        .onClick {
                            world.launch {
                                world.clear()
                            }
                        }
                }
                Button("Load glider gun") {
                    modifier
                        .width(sizes.largeGap * 7F)
                        .margin(horizontal = sizes.gap)
                        .onClick {
                            world.launch {
                                world.loadAsciiState(GameWorld.gliderGun)
                            }
                        }
                }
                Button("Randomize") {
                    modifier
                        .width(sizes.largeGap * 7F)
                        .margin(horizontal = sizes.gap)
                        .onClick {
                            world.launch {
                                world.randomize(0.3F)
                            }
                        }
                }
            }
            Row {
                modifier.padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                Text("Size") { modifier.alignY(AlignmentY.Center).width(sizes.largeGap * 7F) }
                Text("Width:") { modifier.alignY(AlignmentY.Center).margin(horizontal = sizes.gap) }
                Slider(world.width.use().toFloat(), 10F, 1080F) {
                    modifier
                        .alignY(AlignmentY.Center)
                        .margin(horizontal = sizes.gap)
                        .width(sizes.largeGap * 8F)
                        .onChange { world.width.set(it.toInt()) }
                }
                Text("${world.width.value}") { modifier.alignY(AlignmentY.Center).margin(end = sizes.largeGap * 2F) }
                Text("Height:") { modifier.alignY(AlignmentY.Center) }
                Slider(world.height.use().toFloat(), 10F, 720F) {
                    modifier
                        .alignY(AlignmentY.Center)
                        .margin(horizontal = sizes.gap)
                        .width(sizes.largeGap * 8F)
                        .onChange { world.height.set(it.toInt()) }
                }
                Text("${world.height.value}") { modifier.alignY(AlignmentY.Center).margin(end = sizes.largeGap) }
            }
        }
    }

    private inner class RenderPanel : CollapsablePanel("Visualization") {
        override fun UiScope.content() {
            Row {
                modifier.padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                Text("Colors") { modifier.alignY(AlignmentY.Center).width(sizes.largeGap * 7F) }
                ComboBox {
                    modifier
                        .alignY(AlignmentY.Center)
                        .width(sizes.largeGap * 7F)
                        .margin(horizontal = sizes.gap)
                        .items(canvasRenderer.colorChoices)
                        .selectedIndex(canvasRenderer.selectedColor.use())
                        .onItemSelected { canvasRenderer.selectedColor.set(it) }
                }
            }
            Row {
                modifier.padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                Text("Speed") { modifier.alignY(AlignmentY.Center).width(sizes.largeGap * 7F) }
                Slider(world.updateSpeed.use().toFloat(), 0.25F, 240F) {
                    modifier
                        .alignY(AlignmentY.Center)
                        .width(sizes.largeGap * 5F)
                        .margin(horizontal = sizes.gap)
                        .onChange {
                            world.updateSpeed.set(it)
                        }
                }
            }
            Row {
                modifier.padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                Text("Pause game") { modifier.alignY(AlignmentY.Center).width(sizes.largeGap * 7F) }
                Switch(world.paused.use()) {
                    modifier
                        .alignY(AlignmentY.Center)
                        .margin(sizes.gap)
                        .onToggle { world.paused.toggle() }
                }
                Text("Pause on edit") { modifier.alignY(AlignmentY.Center).margin(start = sizes.largeGap * 4F, end = sizes.gap) }
                Switch(isPauseOnEdit.use()) {
                    modifier
                        .alignY(AlignmentY.Center)
                        .margin(sizes.gap)
                        .onToggle { isPauseOnEdit.toggle() }
                }
            }
        }
    }

    private inner class PixelCanvasRenderer : Composable {
        val colorChoices = listOf("Binary", "Oceanic", "Viridis", "Plasma")
        val selectedColor = mutableStateOf(0)

        var canvas = BufferCanvas(world.width.value, world.height.value)

        init {
            world.width.onChange { resize(it, world.height.value) }
            world.height.onChange { resize(world.width.value, it) }
            world.updateCount.onChange { updateCanvas() }
        }

        fun resize(newW: Int, newH: Int) {
            canvas = BufferCanvas(newW, newH)
            updateCanvas()
        }

        private var lastColors = Colors.darkColors()
        val isClassicColor: Boolean
            get() = selectedColor.value == 0

        var parallelism = 12
        private var colorByThread = false
        private var colorGradient: ColorGradient? = null
        private var isEditDrag = false
        private var editChangeToState = false

        private var colorArray = IntArray2D(world.width.value, world.height.value)
        private var lastUpdateJob: Job? = null
        fun updateCanvas() {
            lastUpdateJob?.cancel()
            lastUpdateJob = launch {
                val gameState = world.gameState
                val aliveness = world.aliveness
                val w = gameState.width
                val h = gameState.height
                if (colorArray.width != w || colorArray.height != h)
                    colorArray = IntArray2D(w, h)
                val c = colorArray
                coroutineScope {
                    val ys = h / parallelism
                    for (i in 0..<parallelism) launch {
                        for (y in ys * i..<if (i == parallelism - 1) h else ys * (i + 1)) {
                            ensureActive()
                            for (x in 0..<w) {
                                c[x, y] = cellColor(gameState, aliveness, i, x, y)
                                // c[x, y] = ((255 - i * 255F / parallelism).toInt() and 0xFF shl 16) or ((i * 255F / parallelism).toInt() and 0xFF shl 8) or (if (gameState[x, y] > 0) 0xFF else 0)
                            }
                        }
                    }
                }
                val canvas = canvas
                ensureActive()
                withContext(Dispatchers.RenderLoop) {
                    canvas.loadIntArray(c.asIntArray(), update = true)
                }
            }
        }

        fun cellColor(gameState: ByteArray2D, aliveness: FloatArray, threadId: Int, x: Int, y: Int): Int =
            if (colorByThread)
                ((255 - threadId * 255F / parallelism).toInt() and 0xFF shl 16) or ((threadId * 255F / parallelism).toInt() and 0xFF shl 8) or (if (gameState[x, y] > 0) 0xFF else 0)
            else if (gameState[x, y] > 0) lastColors.primary.RGBA.I
            else colorGradient?.getColor(aliveness[y * gameState.width + x], 0F, 10F)?.RGBA?.I ?: 0

        override fun UiScope.compose(): UiScope {
            world.width.use()
            world.height.use()
            world.updateCount.use()
            colorGradient = when (selectedColor.use()) {
                1 -> ColorGradient.RED_WHITE_BLUE.inverted()
                2 -> ColorGradient.VIRIDIS
                3 -> ColorGradient.PLASMA
                else -> null
            }
            return Canvas(canvas, "Canvas") {
                modifier
                    .onClick {
                        val x = it.position.x.toInt()
                        val y = it.position.y.toInt()
                        world.launchUpdate {
                            gameState[x, y] = 1
                        }
                    }
                    .onDrag {
                        val x = it.position.x.toInt()
                        val y = it.position.y.toInt()
                        world.launchUpdate {
                            gameState[x, y] = 1
                        }
                    }
            }
        }

        private inner class CellCallbacks(val x: Int, val y: Int) {
            val onDragStart: (PointerEvent) -> Unit = {
                isEditDrag = true
                editChangeToState = world.gameState[x, y] == 0.B
                if (isPauseOnEdit.value) world.paused.set(true)
            }

            val onDragEnd: (PointerEvent) -> Unit = {
                isEditDrag = false
            }

            val onPointer: (PointerEvent) -> Unit = {
                if (isEditDrag) {
                    world.launchUpdate {
                        gameState[x, y] = if (editChangeToState) 1 else 0
                    }
                    windowSurface.triggerUpdate()
                }
            }

            val onClick: (PointerEvent) -> Unit = {
                // pause game to enable manual editing
                if (isPauseOnEdit.value) world.paused.set(true)
                world.launchUpdate {
                    gameState[x, y] = if (gameState[x, y] > 0) 0 else 1
                }
                // trigger update (without stepping the game state) to update button state
                windowSurface.triggerUpdate()
            }
        }
    }

    private class GameWorld(ctx: CoroutineContext) : CoroutineScope {
        val width: MutableStateValue<Int> = mutableStateOf(720).onChange { resize(it, height.value) }
        val height: MutableStateValue<Int> = mutableStateOf(480).onChange { resize(width.value, it) }
        val updateCount = mutableStateOf(0)
        val paused = mutableStateOf(false).onChange {
            updatePlayState()
        }
        val updateSpeed = mutableStateOf(60F).onChange {
            loopDelay = maxOf(16L, (1000F / it).toLong())
        }
        private var loopDelay: Long = maxOf(16L, (1000F / updateSpeed.value).toLong())

        override val coroutineContext: CoroutineContext = ctx + Dispatchers.Default

        var parallelism = 8
        private val updateMutex = Mutex()

        private val size = MutableVec2i(width.value, height.value)
        var gameState = ByteArray2D(width.value, height.value)
        private var nextGameState = ByteArray2D(width.value, height.value)
        var aliveness = FloatArray(width.value * height.value)

        private var loopJob: Job? = null
        private fun updatePlayState() {
            loopJob?.cancel()
            loopJob = null
            loopJob = launch {
                while (isActive) {
                    delay(16L)
                    step()
                }
            }
        }

        init {
           updatePlayState()
        }

        private fun resize(newW: Int, newH: Int) = launch {
            update {
                val newState = ByteArray2D(newW, newH)
                nextGameState = ByteArray2D(newW, newH)
                val newAliveness = FloatArray(newW * newH)
                for (y in 0 until min(size.y, newH)) {
                    for (x in 0 until min(size.x, newW)) {
                        newState[x, y] = gameState[x, y]
                        newAliveness[y * newW + x] = aliveness[y * size.x + x]
                    }
                }
                size.x = newW
                size.y = newH
                gameState = newState
                aliveness = newAliveness
            }
        }

        suspend fun step() {
            val ys = size.y / parallelism
            update {
                if (paused.value) return@update
                coroutineScope {
                    for (i in 0 until parallelism) launch {
                        for (y in ys * i until (if (i == parallelism - 1) size.y else ys * (i + 1)))
                            for (x in 0 until size.x) {
                                // nextGameState[y * size.x + x] = ((255 - i * 255 / parallelism).toUInt() shl 16) or ((i * 255 / parallelism).toUInt() shl 8) or 0xFFu
                                val popCnt = countPopNeighbors(x, y)
                                val cell = gameState[x, y]
                                if (nextGameState[x, y] != cell)
                                    aliveness[y * size.x + x] += 1F
                                nextGameState[x, y] = if (popCnt == 2 && cell > 0 || popCnt == 3) 1 else 0
                                aliveness[y * size.x + x] *= 0.99F
                            }
                    }
                }
                gameState = nextGameState.also { nextGameState = gameState }
            }
        }

        private fun countPopNeighbors(x: Int, y: Int): Int {
            var popCnt = 0
            for (iy in -1..1) for (ix in -1..1)
                if (ix != 0 || iy != 0) {
                    val xx = x + ix
                    val yy = y + iy
                    if (0 <= xx && xx < size.x && 0 <= yy && yy < size.y && gameState[x + ix, y + iy] > 0)
                        popCnt++
                }
            return popCnt
        }

        suspend fun clear() = update {
            doClear()
        }

        private fun doClear() {
            nextGameState.asByteArray().fill(0)
            gameState.asByteArray().fill(0)
            aliveness.fill(0F)
        }

        suspend fun randomize(p: Float) = update {
            doClear()
            for (x in 0 until size.x) for (y in 0 until size.y) {
                gameState[x, y] = if (randomF() < p) 1 else 0
            }
        }

        suspend fun loadAsciiState(state: String) = update {
            doClear()
            state.lines().forEachIndexed { y, line ->
                line.forEachIndexed { x, c ->
                    gameState[x, y] = if (c == '0') 1 else 0
                }
            }
        }

        fun update() {
            updateCount.value++
        }

        fun launchUpdate(batch: suspend GameWorld.() -> Unit) = launch {
            update(batch)
        }

        suspend fun update(batch: suspend GameWorld.() -> Unit) = updateMutex.withLock {
            this.batch()
            update()
        }

        companion object {
            val gliderGun = """
            .
            .........................0
            .......................0.0
            .............00......00............00
            ............0...0....00............00
            .00........0.....0...00
            .00........0...0.00....0.0
            ...........0.....0.......0
            ............0...0
            .............00
        """.trimIndent()
        }
    }
}