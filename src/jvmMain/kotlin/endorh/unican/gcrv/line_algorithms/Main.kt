package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig
import de.fabmax.kool.demo.launchSceneLoader
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.Lwjgl3Context

fun main(args: Array<String>) = KoolApplication(
    config = KoolConfig(
        renderBackend = Lwjgl3Context.Backend.OPEN_GL,
        windowTitle = "Prácticas - GCRV",
        windowSize = Vec2i(1080, 640),
        showWindowOnStart = false
    )
) { ctx ->
    var scene: String
    scene = "line-algorithms"
    // scene = "fractals"
    val params = parseArgs(args, mapOf(
        "s" to "scene",
        "p" to "project",
    ))
    params["scene"]?.let { scene = it }
    launchSceneLoader(scene, ctx, loadPhysics = "load-physics" in params, params = params) {
        ctx as Lwjgl3Context
        val window = ctx.renderBackend.glfwWindow
        window.isVisible = true
    }
}

fun parseArgs(args: Array<String>, aliases: Map<String, String> = emptyMap()): Map<String, String> {
    val params: MutableMap<String, String> = mutableMapOf()
    var flag: String? = null
    for (arg in args) {
        val full = arg.startsWith("--")
        if (full || arg.startsWith("-")) {
            if (flag != null && flag !in params) params[flag] = ""
            flag = arg.substring(if (full) 2 else 1)
            while (flag in aliases) flag = aliases[flag]
            continue
        }
        if (flag != null) {
            params[flag] = arg
            flag = null
        }
    }
    if (flag != null && flag !in params) params[flag] = ""
    return params
}