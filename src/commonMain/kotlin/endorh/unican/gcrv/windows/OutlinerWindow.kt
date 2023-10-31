package endorh.unican.gcrv.windows

import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Subdivide
import endorh.unican.gcrv.LineAlgorithmsScene
import endorh.unican.gcrv.line_algorithms.Object2D
import endorh.unican.gcrv.ui2.Section
import endorh.unican.gcrv.util.ModifierState.altPressed
import endorh.unican.gcrv.util.ModifierState.ctrlPressed
import endorh.unican.gcrv.util.ModifierState.shiftPressed
import endorh.unican.gcrv.util.towards

class OutlinerWindow(scene: LineAlgorithmsScene) : BaseWindow("Outliner", scene, true) {

    init {
        windowDockable.setFloatingBounds(width = Dp(150F), height = Dp(400F))
    }

    private val selectedObjects get() = scene.selectedObjects

    override fun UiScope.windowContent() = Column {
        modifier.size(Grow.Std, Grow.Std)
        Row {
            modifier.margin(2.dp).padding(4.dp)
            Button("Delete") {
                modifier.onClick {
                    for (o in scene.selectedObjects)
                        scene.removeObject(o, update = false)
                    scene.selectedObjects.clear()
                    scene.objectStack.objects.firstOrNull()?.let {
                        scene.selectedObjects += it
                    }
                    scene.updateCanvas()
                } //.isEnabled(scene.selectedObjects.use().isNotEmpty())
            }
        }
        LazyList(
            withVerticalScrollbar = false,
            withHorizontalScrollbar = false,
            isScrollableHorizontal = false,
        ) {
            modifier.padding(4.dp)
            itemsIndexed(scene.objectStack.objects.use()) { idx, obj ->
                Row {
                    modifier
                        .width(Grow.Std)
                        .margin(2.dp)
                        .background(
                            RoundRectBackground(
                                if (obj in selectedObjects.use()) colors.backgroundVariant.mix(
                                    Color.WHITE,
                                    0.2F
                                ) else colors.backgroundVariant, 4.dp
                            )
                        )
                        .padding(4.dp)
                        .onClick {
                            if (!ctrlPressed && !shiftPressed)
                                selectedObjects.clear()
                            if (shiftPressed) {
                                selectedObjects.lastOrNull()?.let { last ->
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
                    Text(obj[obj::name].use()) {}
                    Text(obj.type.name) {
                        modifier.width(Grow.Std).textAlignX(AlignmentX.End).textColor(colors.secondary)
                    }
                }
            }
        }
    }
}