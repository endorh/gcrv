package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig
import de.fabmax.kool.demo.demo
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.Lwjgl3Context

fun main() = KoolApplication(
    config = KoolConfig(
        renderBackend = Lwjgl3Context.Backend.OPEN_GL,
        windowTitle = "Kool Demo",
        windowSize = Vec2i(1280, 720),
        // showWindowOnStart = false
    )
) { ctx ->
    // uncomment to load assets locally instead of from web
    // Demo.setProperty("assets.base", ".")

    // sub-directories for individual asset classes within asset base dir
    // Demo.setProperty("assets.hdri", "hdri")
    // Demo.setProperty("assets.materials", "materials")
    // Demo.setProperty("assets.models", "models")

    // launch demo
    demo("line-algorithms", ctx)
}
