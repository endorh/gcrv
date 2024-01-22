package de.fabmax.kool.demo

import de.fabmax.kool.KeyValueStore
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.util.logD

/**
 * Object containing all demo related global settings.
 */
object Settings {

    val defaultUiSizes = mapOf(
        "Small" to UiSizeSetting("Small", Sizes.small),
        "Medium" to UiSizeSetting("Medium", Sizes.medium),
        "Large" to UiSizeSetting("Large", Sizes.large),
    )
    val defaultUiSize = UiSizeSetting("Large", Sizes.large)

    private val settings = mutableListOf<MutableStateSettings<*>>()

    val isFullscreen = MutableStateSettings("gcrv.isFullscreen", false) { it.toBoolean() }
    val showHiddenDemos = MutableStateSettings("gcrv.showHiddenDemos", false) { it.toBoolean() }
    val showDebugOverlay = MutableStateSettings("gcrv.showDebugOverlay", true) { it.toBoolean() }
    val showMenuOnStartup = MutableStateSettings("gcrv.showMenuOnStartup", true) { it.toBoolean() }

    val uiSize = MutableStateSettings("gcrv.uiSize", defaultUiSize) {
        defaultUiSizes[it] ?: defaultUiSize
    }

    val selectedScene = MutableStateSettings("gcrv.selectedScene", Scenes.defaultScene) { it }
    val gizmoSize = MutableStateSettings("gcrv.gizmoSize", 15F) { it.toFloatOrNull() ?: 15F }

    fun loadSettings() {
        settings.forEach { it.load() }
    }

    class MutableStateSettings<T>(
        val key: String, initValue: T, val parser: (String) -> T
    ) : MutableStateValue<T>(initValue) {
        init {
            settings += this
            onChange {
                KeyValueStore.storeString(key, "$it")
                logD { "Stored $key: $it" }
            }
        }

        fun load() {
            KeyValueStore.loadString(key)?.let { set(parser(it)) }
        }
    }

    data class UiSizeSetting(val name: String, val sizes: Sizes) {
        override fun toString(): String = name
    }
}