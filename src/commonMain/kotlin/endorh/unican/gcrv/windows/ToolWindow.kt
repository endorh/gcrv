package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.renderers.OptionPicker
import endorh.unican.gcrv.renderers.OptionalOptionPicker
import endorh.unican.gcrv.scene.LineStyleEditor
import endorh.unican.gcrv.ui2.FixedSection

class ToolWindow(scene: EditorScene) : BaseWindow("Tool", scene, true) {
    init {
        windowDockable.setFloatingBounds(width = Dp(250f), height = Dp(300f))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        FixedSection("Drawing tool") {
            modifier.padding(8.dp)

            OptionalOptionPicker(
                scene.brushTypes, scene.brushType.use(), { scene.brushType.value = it }, "brush"
            ) {
                modifier.width(Grow.Std).margin(bottom=4.dp)
            }

            FixedSection("Style") {
                scene.objectDrawingContext.use()?.apply {
                    drawerStyleEditor()
                } ?: scene.brushType.use()?.objectDrawer?.apply {
                    styleEditor(null)
                }
            }
        }
    }
}