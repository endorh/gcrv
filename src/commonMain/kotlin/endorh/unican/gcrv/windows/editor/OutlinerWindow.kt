package endorh.unican.gcrv.windows.editor

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.scene.objects.GroupObject2D
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.ui2.StringField
import endorh.unican.gcrv.ui2.TRANSPARENT
import endorh.unican.gcrv.util.ModifierState.ctrlPressed
import endorh.unican.gcrv.util.ModifierState.shiftPressed
import endorh.unican.gcrv.util.toTitleCase
import endorh.unican.gcrv.util.towards
import endorh.unican.gcrv.windows.BaseWindow

class OutlinerWindow(scene: EditorScene) : BaseWindow<EditorScene>("Outliner", scene, true) {

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
                    val selected = scene.selectedObjects.toList()
                    group.children.addAll(selected)
                    for (o in selected)
                        scene.removeObject(o, updateCanvas = false)
                    scene.drawObject(group, updateCanvas = false)
                    scene.selectedObjects.clear()
                    scene.selectedObjects += group
                    scene.updateCanvas()
                }
            }
            Button("Delete") {
                modifier.margin(start = 4.dp).onClick {
                    for (o in scene.selectedObjects.toList())
                        scene.removeObject(o, updateCanvas = false)
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
            items(scene.objectStack.objects.use(surface)) { obj ->
                renderItem(obj)
            }
        }
    }

    fun UiScope.renderItem(obj: Object2D) {
        var editingName by remember(false)
        Column(Grow.Std, scopeName = obj.name) {
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
                        if (!ctrlPressed && !shiftPressed) {
                            if (selectedObjects.singleOrNull() == obj) {
                                editingName = true
                                return@onClick
                            }
                            selectedObjects.clear()
                        }
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
                        scene.updateCanvas()
                    }
                if (obj.children.isNotEmpty()) {
                    Arrow(if (obj[obj::collapseChildren].use(surface)) 0F else 90F) {
                        modifier
                            .size(sizes.gap * 1.5F, sizes.gap * 1.5F)
                            .margin(horizontal = sizes.gap)
                            .alignY(AlignmentY.Center)
                            .onClick { obj.collapseChildren = !obj.collapseChildren }
                    }
                }
                if (editingName) {
                    var requestedFocus by remember(false)
                    var editedName by remember(obj.name)
                    StringField(editedName, {
                        editedName = it
                    }, padding=0.dp) {
                        modifier.onEnterPressed {
                            obj.name = editedName
                            requestedFocus = false
                            editingName = false
                        }
                        if (!requestedFocus) {
                            editedName = obj.name
                            requestFocus()
                            requestedFocus = true
                        }
                        isFocused.onChange { if (!it) {
                            editingName = false
                            requestedFocus = false
                        } }
                    }
                } else {
                    Text(obj[obj::name].use(surface)) {}
                }
                Text(obj.type.name.toTitleCase()) {
                    modifier.width(Grow.Std).textAlignX(AlignmentX.End).textColor(colors.secondary)
                }
            }
            if (!obj[obj::collapseChildren].use(surface) && obj.children.isNotEmpty()) Column(Grow.Std) {
                modifier.margin(start=8.dp)
                for (children in obj.children)
                    renderItem(children)
            }
        }
    }
}