package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig
import de.fabmax.kool.demo.launchSceneLoader
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.Lwjgl3Context

fun main() = KoolApplication(
    config = KoolConfig(
        renderBackend = Lwjgl3Context.Backend.OPEN_GL,
        windowTitle = "Prácticas - GCRV",
        windowSize = Vec2i(1080, 640),
        showWindowOnStart = false
    )
) { ctx ->
    // SimpleSceneLoader.setProperty("assets.base", ".")
    // SimpleSceneLoader.setProperty("assets.hdri", "hdri")
    // SimpleSceneLoader.setProperty("assets.materials", "materials")
    // SimpleSceneLoader.setProperty("assets.models", "models")
    var scene: String
    scene = "line-algorithms"
    // scene = "fractals"
    launchSceneLoader(scene, ctx, loadPhysics = false) {
        ctx as Lwjgl3Context
        val window = ctx.renderBackend.glfwWindow
        window.isVisible = true
    }
}
