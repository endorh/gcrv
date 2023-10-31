package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.LineAlgorithmsScene

class InspectorWindow(scene: LineAlgorithmsScene) : BaseWindow("Inspector", scene, true) {

    init {
        windowDockable.setFloatingBounds(width = Dp(150F), height = Dp(400F))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        for (obj in scene.selectedObjects.use()) Row {
            modifier
                .margin(2.dp).padding(4.dp).width(Grow.Std)
                .background(RoundRectBackground(colors.backgroundVariant, 4.dp))
            with(obj) { editor() }
        }
    }
}