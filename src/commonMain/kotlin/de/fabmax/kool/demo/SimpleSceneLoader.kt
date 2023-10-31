package de.fabmax.kool.demo

import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.menu.SceneMenu
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.physics.Physics
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.Time
import endorh.unican.gcrv.util.ModifierState

fun launchSceneLoader(
    startScene: String? = null,
    ctx: KoolContext,
    loadPhysics: Boolean = true,
    onReady: (KoolContext) -> Unit = {}
) {
    var name = startScene
    if (name != null) {
        name = name.lowercase()
        if (name.endsWith("scene"))
            name = name.substring(0, name.length - 4)
    }
    SimpleSceneLoader(ctx, name, loadPhysics, onReady)
}

class SimpleSceneLoader(
    ctx: KoolContext,
    startScene: String? = null,
    loadPhysics: Boolean = true,
    private val onReady: (KoolContext) -> Unit = {}
) {
    val dbgOverlay = DebugOverlay(DebugOverlay.Position.LOWER_RIGHT)
    val menu = SceneMenu(this)

    private val loadingScreen = LoadingScreen(ctx)
    private var currentScene: Pair<String, SimpleScene>? = null
    private var switchEntry: Scenes.Entry? = null

    private var initShownMenu = false
    private var shouldAutoHideMenu = 2.5f

    val activeDemo: SimpleScene?
        get() = currentScene?.second

    private var framesUntilReady = 10

    init {
        // load physics module early - in js, for some reason wasm file cannot be loaded if this happens later on
        if (loadPhysics) Physics.loadPhysics()
        Settings.loadSettings()

        ctx.scenes += dbgOverlay.ui
        ctx.scenes += menu.ui
        ctx.onRender += ::onRender

        val loadScene = startScene ?: Settings.selectedDemo.value
        val loadDemo = Scenes.scenes[loadScene] ?: Scenes.scenes[Scenes.defaultScene]!!
        switchEntry = loadDemo
        ModifierState.registerListeners()
    }

    fun loadScene(entry: Scenes.Entry) {
        if (entry.id != currentScene?.first) {
            switchEntry = entry
        }
    }

    private fun onRender(ctx: KoolContext) {
        applySettings(ctx)
        if (framesUntilReady > 0 && --framesUntilReady <= 0) onReady(ctx)

        switchEntry?.let { newDemo ->
            Settings.selectedDemo.set(newDemo.id)

            // release old demo
            currentScene?.second?.let { scene ->
                scene.scenes.forEach {
                    ctx.scenes -= it
                    it.dispose(ctx)
                }
                scene.menuUi?.let {
                    menu.ui -= it
                    it.dispose(ctx)
                }
                scene.dispose(ctx)
            }
            ctx.scenes.add(0, loadingScreen)

            // set new demo
            currentScene = newDemo.id to newDemo.newInstance(ctx).also {
                it.sceneEntry = newDemo
                it.sceneLoader = this
                it.loadingScreen = loadingScreen
            }
            switchEntry = null
        }

        currentScene?.second?.let {
            if (it.sceneState != SimpleScene.State.RUNNING) {
                it.checkDemoState(this, ctx)
                if (it.sceneState == SimpleScene.State.RUNNING) {
                    // demo setup complete -> add scenes
                    ctx.scenes -= loadingScreen
                    it.scenes.forEachIndexed { i, s -> ctx.scenes.add(i, s) }
                }

            } else {
                // demo fully loaded
                if (shouldAutoHideMenu > 0f) {
                    shouldAutoHideMenu -= Time.deltaT
                    if (Settings.showMenuOnStartup.value) {
                        if (!initShownMenu) {
                            menu.isExpanded = true
                            initShownMenu = true
                        }
                        val ptr = PointerInput.primaryPointer
                        if (shouldAutoHideMenu <= 0f && (!ptr.isValid || ptr.x > UiSizes.menuWidth.px)) {
                            menu.isExpanded = false
                        }
                    }
                }
            }
        }
    }

    private fun applySettings(ctx: KoolContext) {
        if (Settings.isFullscreen.value != ctx.isFullscreen) {
            ctx.isFullscreen = Settings.isFullscreen.value
        }
        dbgOverlay.ui.isVisible = Settings.showDebugOverlay.value
    }

    companion object {
        val props = mutableMapOf<String, Any>()

        val assetStorageBase: String
            get() = getProperty("assets.base", "https://kool.blob.core.windows.net/kool-demo")

        val hdriPath: String
            get() = getProperty("assets.hdri", "$assetStorageBase/hdri")

        val materialPath: String
            get() = getProperty("assets.materials", "$assetStorageBase/materials")

        val modelPath: String
            get() = getProperty("assets.models", "$assetStorageBase/models")

        val heightMapPath: String
            get() = getProperty("assets.heightmaps", "$assetStorageBase/heightmaps")

        val soundPath: String
            get() = getProperty("sounds", "$assetStorageBase/sounds")

        fun setProperty(key: String, value: Any) {
            props[key] = value
        }

        inline fun <reified T> getProperty(key: String, default: T): T {
            return props[key] as? T ?: default
        }
    }
}
