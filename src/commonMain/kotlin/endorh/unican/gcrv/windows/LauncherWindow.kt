package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.LineAlgorithmsScene
import kotlin.reflect.KClass

class LauncherWindow(scene: LineAlgorithmsScene) : BaseWindow("Window Launcher", scene, false) {

    init {
        windowDockable.setFloatingBounds(width = Dp(250f))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        var allowMultiInstances by remember(false)

        Button("UI Basics") {
            launcherButtonStyle("Example window with a few basic UI components")
            modifier.onClick {
                launchOrBringToTop(allowMultiInstances, BasicUiWindow::class) { BasicUiWindow(scene) }
            }
        }
        Button("Text Style") {
            launcherButtonStyle("Signed-distance-field font rendering showcase")
            modifier.onClick {
                launchOrBringToTop(allowMultiInstances, TextStyleWindow::class) { TextStyleWindow(scene) }
            }
        }
        Button("Text Area") {
            launcherButtonStyle("Editable text area with many different text styles")
            modifier.onClick {
                launchOrBringToTop(allowMultiInstances, TextAreaWindow::class) { TextAreaWindow(scene) }
            }
        }
        Button("Conway's Game of Life") {
            launcherButtonStyle("Game of Life simulation / toggle-button benchmark")
            modifier.onClick {
                launchOrBringToTop(allowMultiInstances, GameOfLifeWindow::class) { GameOfLifeWindow(scene) }
            }
        }
        Button("Theme Editor") {
            launcherButtonStyle("UI color theme editor")
            modifier.onClick {
                launchOrBringToTop(false, ThemeEditorWindow::class) { ThemeEditorWindow(scene) }
            }
        }
        Button("Drag and Drop") {
            launcherButtonStyle("Two windows with drag & droppable items")
            modifier.onClick {
                launchOrBringToTop(allowMultiInstances, DragAndDropWindow.A::class) { DragAndDropWindow.A(scene) }
                launchOrBringToTop(allowMultiInstances, DragAndDropWindow.B::class) { DragAndDropWindow.B(scene) }
            }
        }
        Row(Grow.Std) {
            modifier
                .margin(sizes.largeGap)
            Text("Multiple instances") {
                modifier
                    .width(Grow.Std)
                    .alignY(AlignmentY.Center)
                    .onClick { allowMultiInstances = !allowMultiInstances }
            }
            Switch(allowMultiInstances) {
                modifier.onToggle { allowMultiInstances = it }
            }
        }
    }

    private fun <T: BaseWindow> launchOrBringToTop(multiAllowed: Boolean, windowClass: KClass<T>, factory: () -> T) {
        if (!multiAllowed) {
            val existing = scene.demoWindows.find { it::class == windowClass }
            if (existing != null) {
                existing.windowSurface.isFocused.set(true)
                return
            }
        }
        scene.spawnWindow(factory())
    }

    private fun ButtonScope.launcherButtonStyle(tooltip: String) {
        modifier
            .width(Grow.Std)
            .margin(sizes.largeGap)
            .padding(vertical = sizes.gap)

        Tooltip(tooltip)
    }
}