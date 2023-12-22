package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.ui2.Section
import endorh.unican.gcrv.ui2.onClick
import endorh.unican.gcrv.windows.*
import kotlin.reflect.KClass

class FractalsMenuWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("Menu", scene, false) {

   init {
      windowDockable.setFloatingBounds(width = Dp(250f))
   }

   override fun UiScope.windowContent() = ScrollArea(Grow.Std) {
      modifier.width(Grow.Std)
      Column(Grow.Std) {
         var multi by remember(true)

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

            Button("Mandelbrot") {
               launcherButtonStyle("Mandelbrot fractal visualization")
               onClick {
                  launchOrBringToTop(multi) { MandelbrotWindow(scene) }
               }
            }

            Button("Julia") {
               launcherButtonStyle("Mandelbrot fractal visualization")
               onClick {
                  launchOrBringToTop(multi) { JuliaWindow(scene) }
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

   private fun <T : BaseWindow<*>> launchOrBringToTop(multiAllowed: Boolean, windowClass: KClass<T>, factory: () -> T) {
      if (!multiAllowed) {
         val existing = scene.sceneWindows.find { it::class == windowClass }
         if (existing != null) {
            existing.windowSurface.isFocused.set(true)
            return
         }
      }
      scene.spawnWindow(factory())
   }

   private inline fun <reified T : BaseWindow<*>> launchOrBringToTop(multiAllowed: Boolean, noinline factory: () -> T) =
      launchOrBringToTop(multiAllowed, T::class, factory)

   private fun ButtonScope.launcherButtonStyle(tooltip: String) {
      modifier
         .width(Grow.Std)
         .margin(sizes.largeGap)
         .padding(vertical = sizes.gap)
      Tooltip(tooltip)
   }
}