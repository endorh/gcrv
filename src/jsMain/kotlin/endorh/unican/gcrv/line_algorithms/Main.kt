package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig
import de.fabmax.kool.demo.launchSceneLoader
import kotlinx.browser.window
import kotlin.collections.set

fun main() = KoolApplication(
    KoolConfig(isGlobalKeyEventGrabbing = true)
) {
    // SimpleSceneLoader.setProperty("assets.base", ".")
    // SimpleSceneLoader.setProperty("assets.hdri", "hdri")
    // SimpleSceneLoader.setProperty("assets.materials", "materials")
    // SimpleSceneLoader.setProperty("assets.models", "models")


    launchSceneLoader(getParams()["demo"] ?: "line-algorithms", it, loadPhysics = false)
}

@Suppress("UNUSED_VARIABLE")
fun getParams(): Map<String, String> {
    val params: MutableMap<String, String> = mutableMapOf()
    if (window.location.search.length > 1) {
        val vars = window.location.search.substring(1).split("&").filter { it.isNotBlank() }
        for (pair in vars) {
            val keyVal = pair.split("=")
            val keyEnc = keyVal[0]
            val key = js("decodeURIComponent(keyEnc)").toString()
            val value = if (keyVal.size == 2) {
                val valEnc = keyVal[1]
                js("decodeURIComponent(valEnc)").toString()
            } else {
                ""
            }
            params[key] = value
        }
    }
    return params
}