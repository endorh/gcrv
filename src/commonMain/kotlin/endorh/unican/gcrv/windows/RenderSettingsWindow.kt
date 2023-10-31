package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.LineAlgorithmsScene
import endorh.unican.gcrv.line_algorithms.renderers.LineRendererPicker
import endorh.unican.gcrv.line_algorithms.renderers.PointRendererPicker
import endorh.unican.gcrv.line_algorithms.ui.LabeledField
import endorh.unican.gcrv.line_algorithms.ui.LabeledBooleanField
import endorh.unican.gcrv.line_algorithms.ui.LabeledColorField
import endorh.unican.gcrv.line_algorithms.ui.LabeledIntField

class RenderSettingsWindow(scene: LineAlgorithmsScene) : BaseWindow("Render Settings", scene, true) {
    init {
        windowDockable.setFloatingBounds(width = Dp(250f), height = Dp(300f))
    }

    override fun UiScope.windowContent() = ScrollArea(
        vScrollbarModifier = { it.width(2.dp) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std) {
            // modifier.width(Grow.Std)
            LabeledField("Line Renderer") {
                LineRendererPicker(scene.wireframeSettings.fallbackLineRenderer) { it() }
            }
            LabeledBooleanField("Enforce for all lines", scene.wireframeSettings.enforceLineRenderer)
            LabeledColorField("Line Color", scene.wireframeSettings.fallbackLineColor)
            LabeledBooleanField("Enforce for all lines", scene.wireframeSettings.enforceLineColor)

            LabeledField("Point Renderer") {
                PointRendererPicker(scene.wireframeSettings.fallbackPointRenderer) { it() }
            }
            LabeledIntField("Point Size", scene.wireframeSettings.pointSize)
            LabeledBooleanField("Enforce for all points", scene.wireframeSettings.enforcePointRenderer)
            LabeledColorField("Start Point Color", scene.wireframeSettings.startPointColor)
            LabeledColorField("End Point Color", scene.wireframeSettings.endPointColor)
            LabeledBooleanField("Enforce for all points", scene.wireframeSettings.enforcePointColor)
        }
    }
}