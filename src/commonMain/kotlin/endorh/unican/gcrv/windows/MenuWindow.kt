package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.scene.Object2DStack
import endorh.unican.gcrv.serialization.JsonFormat
import endorh.unican.gcrv.ui2.*
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class MenuWindow(scene: EditorScene) : BaseWindow("Menu", scene, false) {

    init {
        windowDockable.setFloatingBounds(width = Dp(250f))
    }

    override fun UiScope.windowContent() = ScrollArea(Grow.Std) {
        modifier.width(Grow.Std)
        Column(Grow.Std) {
            var multi by remember(true)

            Section("Project", true) {
                FixedSection("Load project") {
                    modifier.padding(8.dp)

                    val selectedFile = remember { mutableStateOf<FileReadHandle?>(null) }
                    FilePicker {
                        modifier.width(Grow.Std).margin(2.dp)
                        onFileChosen {
                            selectedFile.value = it
                        }
                    }

                    Button("Load project") {
                        modifier.width(Grow.Std).margin(2.dp)
                        onClick {
                            selectedFile.value?.let {
                                println("File chosen: \"$it\"")
                                launch {
                                    val text = it.readAsText().await()
                                    try {
                                        val stack = JsonFormat.decodeFromString(serializer<Object2DStack>(), text)
                                        scene.loadStack(stack)
                                    } catch (e: Exception) {
                                        println("Failed to load project: $e")
                                        e.printStackTrace()
                                        throw e
                                    }
                                }
                            }
                        }
                    }
                }

                FixedSection("Save project") {
                    modifier.padding(8.dp)
                    FileSaver {
                        modifier.width(Grow.Std).margin(2.dp)
                            .suggestedFileName("scene.json")
                        onFileTextRequested {
                            println("Generating saved file chosen")
                            JsonFormat.encodeToString(serializer<Object2DStack>(), scene.objectStack)
                        }
                    }
                }
            }

            Section("Windows", false) {
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

                Button("Transforms") {
                    launcherButtonStyle("Apply transforms")
                    onClick {
                        launchOrBringToTop(multi) { TransformWindow(scene) }
                    }
                }

                Button("Geo Transforms") {
                    launcherButtonStyle("Apply geometric transforms")
                    onClick {
                        launchOrBringToTop(multi) { GeometryTransformWindow(scene) }
                    }
                }

                Button("Timeline") {
                    launcherButtonStyle("Keyframe timeline")
                    onClick {
                        launchOrBringToTop(multi) { TimeLineWindow(scene) }
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
            }
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