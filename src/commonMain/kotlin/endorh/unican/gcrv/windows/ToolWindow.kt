package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.line_algorithms.ui.LineStyleEditor

class ToolWindow(scene: EditorScene) : BaseWindow("Tool", scene, true) {
    init {
        windowDockable.setFloatingBounds(width = Dp(250f), height = Dp(300f))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        LineStyleEditor(scene.toolLineStyle) {
            modifier.width(Grow.Std)
        }
    }
}