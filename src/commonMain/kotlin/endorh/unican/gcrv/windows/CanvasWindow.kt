package endorh.unican.gcrv.windows

import de.fabmax.kool.input.CursorShape
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.scene.Gizmo2D
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration
import kotlin.time.measureTime

class CanvasWindow(scene: EditorScene) : BaseWindow("Canvas", scene) {
    var lastPoint: Vec2i? = null

    val canvasSize: MutableStateValue<Vec2i> = mutableSerialStateOf(Vec2i(10, 10)).onChange {
        resize(it.x, it.y, origin.value.x, origin.value.y)
    }
    val origin: MutableStateValue<Vec2i> = mutableSerialStateOf(Vec2i(0, 0)).onChange {
        resize(canvasSize.value.x, canvasSize.value.y, it.x, it.y)
    }
    var canvasState = mutableSerialStateOf(makeCanvas(canvasSize.value.x, canvasSize.value.y, origin.value.x, origin.value.y))
    val canvas get() = canvasState.value
    lateinit var canvasScope: CanvasScope

    var updateJob: Job? = null
    var updatePublicationJob: Job? = null

    val pipeline get() = scene.pipeline

    var lastDraggedGizmo: Gizmo2D? = null
    var lastDragStart: Vec2i? = null
    var lastDragOrigin: Vec2i = origin.value

    val lastCanvasUpdateTime = mutableStateOf(Duration.ZERO)

    val popupCanvasCenter = mutableSerialStateOf(Vec2f(0F, 0F))
    val popupCanvasSize = Vec2i(350, 200)
    val popupZoomScale = mutableSerialStateOf(10)
    val popupCanvas = BufferCanvas(popupCanvasSize.x, popupCanvasSize.y)
    val popup: AutoPopup = AutoPopup(hideOnEsc = false, hideOnOutsideClick = false, scopeName = "Zoom")
    init {
        popup.popupContent = Composable {
            modifier.backgroundColor(Color.TRANSPARENT)
            Box {
                modifier
                    .background(RoundRectBackground(colors.backgroundAlpha(0.7F), sizes.gap))
                    .border(RoundRectBorder(colors.primary, sizes.gap, sizes.borderWidth))
                    .padding(sizes.gap)
                Canvas(popupCanvas) {
                    modifier
                        .canvasSize(CanvasSize.FixedScale(1F))
                        .invertY(true)
                        .uvRect(UvRect.FULL)
                        .size(Dp.fromPx(popupCanvasSize.x.F), Dp.fromPx(popupCanvasSize.y.F))
                        .backgroundColor(Color.TRANSPARENT)
                        .onHover {
                            if (ModifierState.altPressed) {
                                val sPos = it.screenPosition
                                val pos = Vec2f(
                                    sPos.x - lastCanvasScope.uiNode.leftPx,
                                    sPos.y - lastCanvasScope.uiNode.topPx)
                                popupCanvasCenter.value = with(canvasScope) {
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
                // println("Update collected! [clear=${it.clear}]")
                // if (updateJob?.isActive == true) {
                //     // pendingUpdate = it.conflate(pendingUpdate)
                //     // return@collectLatest
                //     // println("Previous update is still running")
                //     // delay(0L)
                // }
                // updateJob?.cancel()
                // updateJob = launch {
                    updatePublicationJob?.join()
                    updatePublicationJob = null
                    updateCanvas(it)
                    // pendingUpdate?.let {
                    //     pendingUpdate = null
                    //     println("Rescheduling conflated update")
                    //     // scene.canvasUpdates.emit(it)
                    // }
                // }
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

            canvasScope = Canvas(canvasState.use(), "Canvas") {
                lastCanvasScope = this
                modifier
                    .invertY(true)
                    .uvRect(UvRect.FULL)
                    .canvasSize(CanvasSize.FixedScale(1F))
                    .size(Dp.fromPx(size.x.F), Dp.fromPx(size.y.F))
                    .onClick { ev ->
                        if (ev.pointer.isLeftButtonClicked) {
                            scene.objectDrawingContext.value?.let { ctx ->
                                val pos = Vec2f(ev.canvasPosition)
                                ctx.apply {
                                    dragStart(pos)
                                    drag(pos)
                                    dragEnd(pos)
                                    if (isFinished) {
                                        scene.drawObject(drawnObject)
                                        scene.objectDrawingContext.value = null
                                    }
                                }
                            } ?: scene.brushType.value?.let { type ->
                                val ctx = type.createDrawerContext()?.apply {
                                    val pos = Vec2f(ev.canvasPosition)
                                    dragStart(pos)
                                    drag(pos)
                                    dragEnd(pos)
                                } ?: return@let null
                                scene.objectDrawingContext.value = ctx
                                if (ctx.isFinished) {
                                    scene.drawObject(ctx.drawnObject)
                                    scene.objectDrawingContext.value = null
                                }
                                Unit
                            } ?: run {
                                val pos = ev.canvasPosition
                                scene.objectStack.collectColliders()
                                    .asSequence()
                                    .filter { it.first.fastContains(pos.toVec2i()) }
                                    .sortedBy { it.first.center.toVec2f().distance(pos) }
                                    .firstOrNull()?.let { (c, o) ->
                                        if (ModifierState.ctrlPressed) {
                                            scene.selectedObjects.toggle(o)
                                        } else {
                                            scene.selectedObjects.atomic {
                                                clear()
                                                add(o)
                                            }
                                        }
                                        scene.updateCanvas()
                                    } ?: run {
                                        scene.selectedObjects.clear()
                                        scene.updateCanvas()
                                    }
                            }
                        } else if (ev.pointer.isRightButtonClicked) {
                            scene.objectDrawingContext.value?.apply {
                                rightClick()
                                if (isFinished) {
                                    scene.drawObject(drawnObject)
                                    scene.objectDrawingContext.value = null
                                }
                                Unit
                            }
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
                        scene.objectDrawingContext.value?.apply {
                            hover(it.canvasPosition)
                            // We don't allow ending a draw operation on hover
                        }
                    }
                    .onDragStart { ev ->
                        if (ev.pointer.isLeftButtonDown) {
                            scene.objectDrawingContext.value?.apply {
                                val pos = Vec2f(ev.canvasPosition)
                                dragStart(pos)
                                drag(pos)
                                if (isFinished) {
                                    scene.drawObject(drawnObject)
                                    scene.objectDrawingContext.value = null
                                }
                            } ?: run {
                                scene.brushType.value?.let { type ->
                                    type.createDrawerContext()?.apply {
                                        val pos = Vec2f(ev.canvasPosition)
                                        dragStart(pos)
                                        drag(pos)
                                        scene.objectDrawingContext.value = this
                                    }
                                } ?: run {
                                    val pos = ev.canvasPosition.toVec2i()
                                    scene.selectedObjects.asSequence()
                                        .flatMap { it.gizmos.asSequence() }
                                        .filter { pos in it.collider }
                                        .sortedBy { it.collider.centerDistance(pos) }
                                        .firstOrNull()?.let { gizmo ->
                                            lastDraggedGizmo = gizmo
                                            return@onDragStart
                                        }
                                }
                            }
                        }
                        if (ev.pointer.isMiddleButtonDown || ev.pointer.isRightButtonDown) {
                            lastDragStart = ev.position.toVec2i()
                            lastDragOrigin = origin.value
                            PointerInput.cursorShape = CursorShape.HAND
                        } else lastDragStart = null
                    }
                    .onDrag {
                        lastDraggedGizmo?.let { g ->
                            g.drag(it.canvasPosition)
                        } ?: lastDragStart?.let { s ->
                            val (sx, sy) = s
                            val (x, y) = it.position.round
                            origin.value = Vec2i(lastDragOrigin.x + sx - x, lastDragOrigin.y - sy + y)
                            PointerInput.cursorShape = CursorShape.HAND
                        } ?: scene.objectDrawingContext.value?.apply {
                            drag(it.canvasPosition)
                        }
                    }
                    .onDragEnd {
                        lastDraggedGizmo = null
                        if (lastDragStart != null) {
                            PointerInput.cursorShape = CursorShape.DEFAULT
                        } else scene.objectDrawingContext.value?.apply {
                            val pos = Vec2f(it.canvasPosition)
                            drag(pos)
                            dragEnd(pos)
                            if (isFinished) {
                                scene.drawObject(drawnObject)
                                scene.objectDrawingContext.value = null
                            }
                        }
                        lastDragStart = null
                        lastDragOrigin = origin.value
                    }
            }
            popup()
            Text("${lastCanvasUpdateTime.use().inWholeMilliseconds} ms") {
                modifier.alignX(AlignmentX.End).alignY(AlignmentY.Bottom)
                    .padding(1.dp).zLayer(10)
                    .textColor(Color.GRAY)
            }
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
                F[i, j] = canvas.R.F[(l + i / s).I, (t + j / s).I]

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

    fun updateCanvas(event: EditorScene.CanvasUpdateEvent) {
        // println("Canvas update queued for the render loop!")
        doUpdateCanvas(event)
        // If we attempt to upload textures to GPU from a work thread OpenGL can crash
        // launchOnMainThread {  }
        // withContext(Dispatchers.RenderLoop) {
        //     println("> Canvas update has reached the render loop")
        //     doUpdateCanvas(event)
        //     println("< Canvas update is done on the render loop")
        // }
    }

    fun doUpdateCanvas(event: EditorScene.CanvasUpdateEvent) {
        lastCanvasUpdateTime.value = measureTime {
            if (event.clear) pipeline.clear(canvas, false)
            pipeline.render(canvas, false)
        }
        // Uploading a texture to GPU apparently needs to happen on the render thread
        updatePublicationJob = launchOnMainThread {
            canvas.update()
        }
    }
}