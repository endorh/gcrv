package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.objects.GroupObject2D
import endorh.unican.gcrv.objects.Object2D
import endorh.unican.gcrv.ui2.TRANSPARENT
import endorh.unican.gcrv.util.ModifierState.ctrlPressed
import endorh.unican.gcrv.util.ModifierState.shiftPressed
import endorh.unican.gcrv.util.towards

class OutlinerWindow(scene: EditorScene) : BaseWindow("Outliner", scene, true) {

    init {
        windowDockable.setFloatingBounds(width = Dp(150F), height = Dp(400F))
    }

    private val selectedObjects get() = scene.selectedObjects

    override fun UiScope.windowContent() = Column {
        modifier.size(Grow.Std, Grow.Std)
        Row {
            modifier.margin(2.dp).padding(4.dp)

            Button("Group") {
                modifier.onClick {
                    val group = GroupObject2D()
                    group.children.addAll(scene.selectedObjects)
                    for (o in scene.selectedObjects)
                        scene.removeObject(o, update = false)
                    scene.drawObject(group, update = false)
                    scene.selectedObjects.clear()
                    scene.selectedObjects += group
                    scene.updateCanvas()
                }
            }
            Button("Delete") {
                modifier.margin(start = 4.dp).onClick {
                    for (o in scene.selectedObjects)
                        scene.removeObject(o, update = false)
                    scene.selectedObjects.clear()
                    scene.objectStack.objects.firstOrNull()?.let {
                        scene.selectedObjects += it
                    }
                    scene.updateCanvas()
                }
            }
        }
        LazyList(
            withVerticalScrollbar = false,
            withHorizontalScrollbar = false,
            isScrollableHorizontal = false,
        ) {
            modifier.padding(4.dp)
            itemsIndexed(scene.objectStack.objects.use()) { idx, obj ->
                renderItem(obj)
            }
        }
    }

    fun UiScope.renderItem(obj: Object2D) {
        Column(Grow.Std) {
            Row(Grow.Std) {
                modifier
                    .margin(2.dp)
                    .background(
                        RoundRectBackground(
                            if (obj in selectedObjects.use()) colors.backgroundVariant.mix(
                                Color.WHITE,
                                0.2F
                            ) else Color.TRANSPARENT, 4.dp
                        )
                    )
                    .padding(4.dp)
                    .onClick {
                        if (!ctrlPressed && !shiftPressed)
                            selectedObjects.clear()
                        if (shiftPressed) {
                            selectedObjects.lastOrNull()?.let { last ->
                                val idx = scene.objectStack.objects.indexOf(obj)
                                if (idx < 0) return@let
                                val lastIdx = scene.objectStack.objects.indexOf(last).let {
                                    if (it == -1) idx else it
                                }
                                for (i in lastIdx towards idx) {
                                    val o = scene.objectStack.objects[i]
                                    if (o !in selectedObjects) selectedObjects.add(o)
                                }
                            } ?: selectedObjects.add(obj)
                        } else if (ctrlPressed) {
                            if (obj in selectedObjects) selectedObjects.remove(obj)
                            else selectedObjects.add(obj)
                        } else selectedObjects.add(obj)
                    }
                Text(obj[obj::name].use(surface)) {}
                Text(obj.type.name) {
                    modifier.width(Grow.Std).textAlignX(AlignmentX.End).textColor(colors.secondary)
                }
            }
            if (obj.children.isNotEmpty()) Column(Grow.Std) {
                modifier.margin(start=8.dp)
                for (children in obj.children)
                    renderItem(children)
            }
        }
    }
}