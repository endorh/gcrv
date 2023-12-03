package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.ui2.LabeledField
import endorh.unican.gcrv.ui2.LabeledBooleanField
import endorh.unican.gcrv.ui2.LabeledColorField
import endorh.unican.gcrv.ui2.LabeledIntField
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
                LabeledBooleanField("Grid", scene.gridPass.enabled)
                LabeledBooleanField("Axes", scene.axesPass.enabled)
                LabeledBooleanField("Geo Splines", scene.geoSplinePass.enabled)
                LabeledBooleanField("Geo Wires", scene.geoWireframePass.enabled)
                LabeledBooleanField("Geo Points", scene.geoPointPass.enabled)
                LabeledBooleanField("Splines", scene.splinePass.enabled)
                LabeledBooleanField("Wireframe", scene.wireframePass.enabled)
                LabeledBooleanField("Points", scene.pointPass.enabled)
                LabeledBooleanField("Gizmos", scene.gizmoPass.enabled)
            }
            Section("Spline Style") {
                LabeledField("Renderer") {
                    OptionPicker(CubicSplineRenderers, scene.cubicSplineRenderingSettings.fallbackRenderer.use(), { scene.cubicSplineRenderingSettings.fallbackRenderer.value = it }) { it() }
                }
                LabeledBooleanField("Enforce", scene.cubicSplineRenderingSettings.enforceRenderer)
                LabeledColorField("Color", scene.cubicSplineRenderingSettings.fallbackColor)
                LabeledBooleanField("Enforce", scene.cubicSplineRenderingSettings.enforceColor)
            }
            Section("Line Style") {
                LabeledField("Renderer") {
                    OptionPicker(LineRenderers, scene.wireframeSettings.fallbackRenderer.use(), { scene.wireframeSettings.fallbackRenderer.value = it }) { it() }
                }
                LabeledBooleanField("Enforce", scene.wireframeSettings.enforceRenderer)
                LabeledColorField("Color", scene.wireframeSettings.fallbackColor)
                LabeledBooleanField("Enforce", scene.wireframeSettings.enforceColor)
            }
            Section("Point Style") {
                LabeledField("Renderer") {
                    OptionPicker(PointRenderers, scene.pointSettings.fallbackRenderer.use(), { scene.pointSettings.fallbackRenderer.value = it }) { it() }
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