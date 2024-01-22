package endorh.unican.gcrv

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfig
import de.fabmax.kool.demo.launchSceneLoader
import endorh.unican.gcrv.util.decodeURIComponent
import kotlinx.browser.window
import kotlin.collections.set

fun main() = KoolApplication(
    KoolConfig(isGlobalKeyEventGrabbing = true)
) {
    val params = getParams()
    println("Params: $params")
    launchSceneLoader(params["scene"] ?: "line-algorithms", it, loadPhysics = false, params = params)
}

fun getParams(): Map<String, String> {
    val params: MutableMap<String, String> = mutableMapOf()
    if (window.location.search.length > 1) {
        val vars = window.location.search.substring(1).split("&").filter { it.isNotBlank() }
        for (pair in vars) {
            val keyVal = pair.split("=")
            val keyEnc = keyVal[0]
            val key = decodeURIComponent(keyEnc)
            val value = if (keyVal.size == 2) {
                val valEnc = keyVal[1]
                decodeURIComponent(valEnc)
            } else ""
            params[key] = value
        }
    }
    return params
}