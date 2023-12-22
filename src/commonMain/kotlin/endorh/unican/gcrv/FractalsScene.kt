package endorh.unican.gcrv

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.SimpleScene
import de.fabmax.kool.demo.SimpleSceneLoader
import de.fabmax.kool.demo.UiSizes
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.pipeline.FullscreenShaderUtil
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.windows.BaseWindow
import endorh.unican.gcrv.windows.fractals.FractalsMenuWindow
import endorh.unican.gcrv.windows.fractals.JuliaWindow
import endorh.unican.gcrv.windows.fractals.MandelbrotWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class FractalsScene : SimpleScene("Fractals"), WindowScene, CoroutineScope {
   override val coroutineContext: CoroutineContext = Dispatchers.Default

   override val dock = Dock()
   override val sceneWindows = mutableListOf<BaseWindow<*>>()
   override val selectedUiSize = mutableStateOf(Sizes.medium)
   override val selectedColors: MutableStateValue<Colors> = mutableStateOf(
      Colors.darkColors(
         Color("55A4CDFF"),
         Color("789DA6F0"),
         Color("6797A4FF"),
         Color("69797CFF"),
         Color("2B2B2BFF"),
         Color("64646438"),
         Color("FFFFFFFF"),
         Color("FFFFFFFF"),
         Color("D4DBDDFF"),
      )
   ).onChange { dock.dockingSurface.colors = it }
   override val windowSpawnLocation = MutableVec2f(320F, 32F)

   var exampleImage: Texture2d? = null

   override suspend fun Assets.loadResources(ctx: KoolContext) {
      exampleImage = loadTexture2d("${SimpleSceneLoader.materialPath}/uv_checker_map.jpg")
   }

   override fun Scene.setupMainScene(ctx: KoolContext) {
      setupUiScene(true)

      dock.dockingSurface.colors = selectedColors.value
      dock.dockingPaneComposable = Composable {
         Box(Grow.Std, Grow.Std) {
            modifier.margin(start = UiSizes.baseSize)
            dock.root()
         }
      }

      addNode(dock)

      dock.createNodeLayout(
         listOf(
            "0:row",
            "0:row/0:col",
            "0:row/0:col/0:leaf",
            // "0:row/0:col/1:leaf",
            "0:row/1:col",
            "0:row/1:col/0:leaf",
            // "0:row/1:col/1:leaf",
            // "0:row/2:col",
            // "0:row/2:col/0:leaf",
            // "0:row/2:col/1:leaf"
         )
      )

      // Set relative proportions
      dock.getNodeAtPath("0:row/0:col")?.width?.value = Grow(0.25F)
      dock.getNodeAtPath("0:row/1:col")?.width?.value = Grow(1F)
      // dock.getNodeAtPath("0:row/2:col")?.width?.value = Grow(0.25F)

      // Add a hidden empty dockable to the center node to avoid, center node being removed on undock
      val centerSpacer = UiDockable("EmptyDockable", dock, isHidden = true)
      dock.getLeafAtPath("0:row/1:col/0:leaf")?.dock(centerSpacer)

      // Spawn initial windows
      val scene = this@FractalsScene
      spawnWindow(FractalsMenuWindow(scene), "0:row/0:col/0:leaf")
      spawnWindow(MandelbrotWindow(scene), "0:row/1:col/0:leaf")
      spawnWindow(JuliaWindow(scene), "0:row/1:col/0:leaf")
   }
}