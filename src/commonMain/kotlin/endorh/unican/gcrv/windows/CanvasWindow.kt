package endorh.unican.gcrv.windows

import de.fabmax.kool.input.CursorShape
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.RenderLoop
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.objects.LineObject2D
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CanvasWindow(scene: EditorScene) : BaseWindow("Canvas", scene) {
    var lastPoint: Vec2i? = null

    val canvasSize: MutableStateValue<Vec2i> = mutableStateOf(Vec2i(10, 10)).onChange {
        resize(it.x, it.y, origin.value.x, origin.value.y)
    }
    val origin: MutableStateValue<Vec2i> = mutableStateOf(Vec2i(0, 0)).onChange {
        resize(canvasSize.value.x, canvasSize.value.y, it.x, it.y)
    }
    var canvasState = mutableStateOf(makeCanvas(canvasSize.value.x, canvasSize.value.y, origin.value.x, origin.value.y))
    val canvas get() = canvasState.value

    val pipeline get() = scene.pipeline

    var lastDragStart: Vec2i? = null
    var lastDragOrigin: Vec2i = origin.value

    val popupCanvasCenter = mutableStateOf(Vec2f(0F, 0F))
    val popupCanvasSize = Vec2i(350, 200)
    val popupZoomScale = mutableStateOf(10)
    val popupCanvas = BufferCanvas(popupCanvasSize.x, popupCanvasSize.y)
    val popup: AutoPopup = AutoPopup(hideOnEsc = false, hideOnOutsideClick = false, scopeName = "Zoom")
    init {
        popup.popupContent = Composable {
            modifier.backgroundColor(Color.TRANSPARENT)
            Box {
                modifier
                    .background(RoundRectBackground(colors.backgroundVariant.withAlpha(0.7F), sizes.gap))
                    .border(RoundRectBorder(colors.primary, sizes.gap, sizes.borderWidth))
                    .padding(sizes.gap)
                Canvas(popupCanvas) {
                    modifier
                        .canvasSize(CanvasSize.FixedScale(1F))
                        .size(Dp.fromPx(popupCanvasSize.x.F), Dp.fromPx(popupCanvasSize.y.F))
                        .backgroundColor(Color.TRANSPARENT)
                        .onHover {
                            if (ModifierState.altPressed) {
                                val sPos = it.screenPosition
                                val pos = Vec2f(
                                    sPos.x - lastCanvasScope.uiNode.leftPx,
                                    sPos.y - lastCanvasScope.uiNode.topPx)
                                popupCanvasCenter.value = with (canvas) {
                                    pos.toCanvasCoordinates()
                                }
                                val popupPos = it.screenPosition.subtract(
                                    Vec2f(popupCanvasSize.x / 2 + sizes.gap.px, popupCanvasSize.y / 2 + sizes.gap.px),
                                    MutableVec2f()
                                )
                                updateZoomCanvas()
                                popup.show(popupPos)
                                PointerInput.cursorShape = CursorShape.CROSSHAIR
                            } else if (popup.isVisible.value) {
                                popup.hide()
                                PointerInput.cursorShape = CursorShape.DEFAULT
                            }
                        }
                        .onWheelY {
                            val delta = it.pointer.deltaScrollY.toInt()
                            popupZoomScale.value = maxOf(2, minOf(50, popupZoomScale.value + delta))
                            updateZoomCanvas()
                        }
                }
            }
        }
    }

    init {
        windowDockable.setFloatingBounds(width = Dp(400F), height = Dp(300F))

        launch {
            scene.canvasUpdates.collectLatest {
                updateCanvas(it)
            }
        }
    }

    protected fun makeCanvas(width: Int, height: Int, centerX: Int, centerY: Int) = BufferCanvas(width, height).apply {
        origin.set(Vec2i(centerX - width / 2, centerY - height / 2))
    }

    protected fun resize(width: Int, height: Int, centerX: Int, centerY: Int) {
        canvasState.value = makeCanvas(width, height, centerX, centerY)
        doUpdateCanvas(EditorScene.CanvasUpdateEvent())
    }

    private lateinit var lastCanvasScope: CanvasScope

    override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
        Box {
            modifier
                .margin(sizes.gap)
                .size(Grow.Std, Grow.Std)
                .backgroundColor(colors.backgroundVariant)
                .onPositioned { checkSize(it) }

            val size = checkSize(uiNode)

            Canvas(canvasState.use(), "Canvas") {
                lastCanvasScope = this
                modifier
                    .invertY(true)
                    .uvRect(UvRect.FULL)
                    .canvasSize(CanvasSize.FixedScale(1F))
                    .size(Dp.fromPx(size.x.F), Dp.fromPx(size.y.F))
                    .onClick {
                        if (it.pointer.isLeftButtonClicked) {
                            val (x, y) = it.canvasPosition.round
                            val last = lastPoint
                            canvas.update {
                                F.C[x, y] = Color.WHITE
                            }
                            lastPoint = if (last != null) {
                                scene.drawObject(LineObject2D(last, Vec2i(x, y), scene.toolLineStyle.value.copy()))
                                null
                            } else Vec2i(x, y)
                        }
                    }
                    .onHover {
                        if (ModifierState.altPressed) {
                            popupCanvasCenter.value = it.canvasPosition
                            val popupPos = it.screenPosition.subtract(
                                Vec2f(popupCanvasSize.x / 2 + sizes.gap.px, popupCanvasSize.y / 2 + sizes.gap.px),
                                MutableVec2f())
                            updateZoomCanvas()
                            popup.show(popupPos)
                            PointerInput.cursorShape = CursorShape.CROSSHAIR
                        } else if (popup.isVisible.value) {
                            popup.hide()
                            PointerInput.cursorShape = CursorShape.DEFAULT
                        }
                    }
                    .onDragStart {
                        if (it.pointer.isMiddleButtonDown || it.pointer.isRightButtonDown) {
                            lastDragStart = it.position.toVec2i()
                            lastDragOrigin = origin.value
                            PointerInput.cursorShape = CursorShape.HAND
                        } else lastDragStart = null
                    }
                    .onDrag {
                        lastDragStart?.let { s ->
                            val (sx, sy) = s
                            val (x, y) = it.position.round
                            origin.value = Vec2i(lastDragOrigin.x + sx - x, lastDragOrigin.y - sy + y)
                            PointerInput.cursorShape = CursorShape.HAND
                        }
                    }
                    .onDragEnd {
                        if (lastDragStart != null)
                            PointerInput.cursorShape = CursorShape.DEFAULT
                        lastDragStart = null
                        lastDragOrigin = origin.value
                    }
            }
            popup()
        }
    }

    private fun updateZoomCanvas() {
        popupCanvas.update {
            clear()
            val (w, h) = popupCanvasSize
            val (cx, cy) = popupCanvasCenter.value
            val s = popupZoomScale.value

            val l = cx - w / 2 / s
            val t = cy - h / 2 / s
            for (i in 0..<w) for (j in 0..<h)
                F[i, j] = canvas.F[(l + i / s).I, (t + j / s).I]

            val iw = w / s
            val ih = h / s
            val il = w / 2 - iw / 2
            val it = h / 2 - ih / 2
            val c = Color.LIGHT_GRAY.RGBA.I
            for (i in il..il + iw) {
                F[i, it] = c
                F[i, it + ih] = c
            }
            for (j in it..it + ih) {
                F[il, j] = c
                F[il + iw, j] = c
            }
        }
    }

    private fun checkSize(uiNode: UiNode): Vec2i {
        var size = canvasSize.value
        val w = uiNode.innerWidthPx.I
        val h = uiNode.innerHeightPx.I
        if (w != size.x || h != size.y)
            canvasSize.value = Vec2i(w, h).also { size = it }
        return size
    }

    suspend fun updateCanvas(event: EditorScene.CanvasUpdateEvent) {
        // If we attempt to upload textures to GPU from a work thread OpenGL can crash
        withContext(Dispatchers.RenderLoop) {
            doUpdateCanvas(event)
        }
    }

    fun doUpdateCanvas(event: EditorScene.CanvasUpdateEvent) {
        if (event.clear) pipeline.clear(canvas)
        pipeline.render(canvas)
    }
}