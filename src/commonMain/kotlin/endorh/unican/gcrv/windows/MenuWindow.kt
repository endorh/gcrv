package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.ui2.Section
import endorh.unican.gcrv.ui2.onClick
import kotlin.reflect.KClass

class MenuWindow(scene: EditorScene) : BaseWindow("Menu", scene, false) {

    init {
        windowDockable.setFloatingBounds(width = Dp(250f))
    }

    override fun UiScope.windowContent() = Column(Grow.Std) {
        var multi by remember(true)

        Button("Canvas") {
            launcherButtonStyle("Canvas window")
            onClick {
                launchOrBringToTop(multi) { CanvasWindow(scene) }
            }
        }
        Button("Tool") {
            launcherButtonStyle("Drawing tool settings")
            onClick {
                launchOrBringToTop(multi) { ToolWindow(scene) }
            }
        }
        Button("Outliner") {
            launcherButtonStyle("Object selector")
            onClick {
                launchOrBringToTop(multi) { OutlinerWindow(scene) }
            }
        }
        Button("Inspector") {
            launcherButtonStyle("Inspect and edit canvas objects")
            onClick {
                launchOrBringToTop(multi) { InspectorWindow(scene) }
            }
        }
        Button("Render Settings") {
            launcherButtonStyle("Viewport settings")
            onClick {
                launchOrBringToTop(multi) { RenderSettingsWindow(scene) }
            }
        }


        Section("Settings") {
            Row(Grow.Std) {
                modifier
                    .margin(sizes.largeGap)
                Text("Multiple instances") {
                    modifier
                        .width(Grow.Std)
                        .alignY(AlignmentY.Center)
                        .onClick { multi = !multi }
                }
                Switch(multi) {
                    modifier.onToggle { multi = it }
                }
            }
        }

        Section("Demo Windows", false) {
            Button("UI Basics") {
                launcherButtonStyle("Example window with a few basic UI components")
                onClick {
                    launchOrBringToTop(multi) { BasicUiWindow(scene) }
                }
            }
            // Button("Text Style") {
            //     launcherButtonStyle("Signed-distance-field font rendering showcase")
            //     onClick {
            //         launchOrBringToTop(multi) { TextStyleWindow(scene) }
            //     }
            // }
            // Button("Text Area") {
            //     launcherButtonStyle("Editable text area with many different text styles")
            //     onClick {
            //         launchOrBringToTop(multi) { TextAreaWindow(scene) }
            //     }
            // }
            Button("Game of Life (UI)") {
                launcherButtonStyle("Conway's Game of Life simulation / toggle-button benchmark")
                onClick {
                    launchOrBringToTop(multi) { GameOfLifeWindow(scene) }
                }
            }
            Button("Game of Life (Canvas)") {
                launcherButtonStyle("Conway's Game of Life simulation / canvas test")
                onClick {
                    launchOrBringToTop(multi) { GameOfLifeCanvasWindow(scene) }
                }
            }

            Button("Theme Editor") {
                launcherButtonStyle("UI color theme editor")
                onClick {
                    launchOrBringToTop(false) { ThemeEditorWindow(scene) }
                }
            }
            // Button("Drag and Drop") {
            //     launcherButtonStyle("Two windows with drag & droppable items")
            //     onClick {
            //         launchOrBringToTop(multi) { DragAndDropWindow.A(scene) }
            //         launchOrBringToTop(multi) { DragAndDropWindow.B(scene) }
            //     }
            // }
        }
    }

    private fun <T: BaseWindow> launchOrBringToTop(multiAllowed: Boolean, windowClass: KClass<T>, factory: () -> T) {
        if (!multiAllowed) {
            val existing = scene.subWindows.find { it::class == windowClass }
            if (existing != null) {
                existing.windowSurface.isFocused.set(true)
                return
            }
        }
        scene.spawnWindow(factory())
    }
    private inline fun <reified T: BaseWindow> launchOrBringToTop(multiAllowed: Boolean, noinline factory: () -> T) =
        launchOrBringToTop(multiAllowed, T::class, factory)

    private fun ButtonScope.launcherButtonStyle(tooltip: String) {
        modifier
            .width(Grow.Std)
            .margin(sizes.largeGap)
            .padding(vertical = sizes.gap)

        Tooltip(tooltip)
    }
}