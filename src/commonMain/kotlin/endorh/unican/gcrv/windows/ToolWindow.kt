package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.LineAlgorithmsScene
import endorh.unican.gcrv.line_algorithms.LineStyle
import endorh.unican.gcrv.line_algorithms.ui.LineStyleEditor
import kotlin.reflect.KClass

class ToolWindow(scene: LineAlgorithmsScene) : BaseWindow("Tool", scene, true) {
    init {
        windowDockable.setFloatingBounds(width = Dp(250f), height = Dp(300f))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        LineStyleEditor(scene.toolLineStyle)
    }
}