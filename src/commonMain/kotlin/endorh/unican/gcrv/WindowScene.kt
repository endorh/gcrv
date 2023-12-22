package endorh.unican.gcrv

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.util.launchDelayed
import endorh.unican.gcrv.windows.BaseWindow
import kotlinx.coroutines.CoroutineScope

interface WindowScene : CoroutineScope {
   val dock: Dock
   val selectedUiSize: MutableStateValue<Sizes>
   val selectedColors: MutableStateValue<Colors>
   val sceneWindows: MutableList<BaseWindow<*>>
   val windowSpawnLocation: MutableVec2f

   fun spawnWindow(window: BaseWindow<*>, dockPath: String? = null) {
      sceneWindows += window

      dock.addDockableSurface(window.windowDockable, window.windowSurface)
      dockPath?.let { dock.getLeafAtPath(it)?.dock(window.windowDockable) }

      window.windowDockable.setFloatingBounds(Dp(windowSpawnLocation.x), Dp(windowSpawnLocation.y))
      windowSpawnLocation.x += 32F
      windowSpawnLocation.y += 32F

      if (windowSpawnLocation.y > 480F) {
         windowSpawnLocation.y -= 416
         windowSpawnLocation.x -= 384
         if (windowSpawnLocation.x > 480F)
            windowSpawnLocation.x = 320F
      }

      launchDelayed(1) {
         window.windowSurface.isFocused.set(true)
      }
   }

   fun closeWindow(window: BaseWindow<*>, ctx: KoolContext) {
      dock.removeDockableSurface(window.windowSurface)
      sceneWindows -= window
      window.onClose()
      window.windowSurface.dispose(ctx)
   }
}