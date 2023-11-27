package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.line_algorithms.renderers.LineRendererPicker
import endorh.unican.gcrv.line_algorithms.renderers.PointRendererPicker
import endorh.unican.gcrv.line_algorithms.ui.LabeledField
import endorh.unican.gcrv.line_algorithms.ui.LabeledBooleanField
import endorh.unican.gcrv.line_algorithms.ui.LabeledColorField
import endorh.unican.gcrv.line_algorithms.ui.LabeledIntField
import endorh.unican.gcrv.ui2.Section

class RenderSettingsWindow(scene: EditorScene) : BaseWindow("Render Settings", scene, true) {
    init {
        windowDockable.setFloatingBounds(width = Dp(250f), height = Dp(300f))
    }

    override fun UiScope.windowContent() = ScrollArea(
        vScrollbarModifier = { it.width(2.dp) }
    ) {
        modifier.width(Grow.Std)
        Column(Grow.Std) {
            Section("Layers") {
                LabeledBooleanField("Axes", scene.axesPass.enabled)
                // LabeledBooleanField("Grid", scene.gridPass.enabled, { scene.gridPass.enabled = it })
                LabeledBooleanField("Geo Wires", scene.geoWireframePass.enabled)
                LabeledBooleanField("Geo Points", scene.geoPointPass.enabled)
                LabeledBooleanField("Wireframe", scene.wireframePass.enabled)
                LabeledBooleanField("Points", scene.pointPass.enabled)
            }
            Section("Line Style") {
                LabeledField("Renderer") {
                    LineRendererPicker(scene.wireframeSettings.fallbackRenderer) { it() }
                }
                LabeledBooleanField("Enforce", scene.wireframeSettings.enforceRenderer)
                LabeledColorField("Color", scene.wireframeSettings.fallbackColor)
                LabeledBooleanField("Enforce", scene.wireframeSettings.enforceColor)
            }
            Section("Point Style") {
                LabeledField("Renderer") {
                    PointRendererPicker(scene.pointSettings.fallbackRenderer) { it() }
                }
                LabeledBooleanField("Enforce", scene.pointSettings.enforceRenderer)
                LabeledIntField("Size", scene.pointSettings.fallbackSize)
                LabeledBooleanField("Enforce", scene.pointSettings.enforceSize)
                LabeledColorField("Color", scene.pointSettings.fallbackColor)
                LabeledBooleanField("Enforce", scene.pointSettings.enforceColor)
            }
        }
    }
}