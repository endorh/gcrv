package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.UiDockable
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.WindowScene
import kotlinx.coroutines.*

abstract class BaseWindow<out Scene: WindowScene>(name: String, val scene: Scene, isClosable: Boolean = true) : CoroutineScope {
    override val coroutineContext = scene.coroutineContext
    val windowDockable = UiDockable(name, scene.dock)

    val windowSurface = WindowSurface(windowDockable) {
        surface.sizes = scene.selectedUiSize.use()
        surface.colors = scene.selectedColors.use()

        modifyWindow()

        var isMinimizedToTitle by remember(false)
        val isDocked = windowDockable.isDocked.use()

        Column(Grow.Std, Grow.Std) {
            TitleBar(
                windowDockable,
                isMinimizedToTitle = isMinimizedToTitle,
                onMinimizeAction = if (!isDocked && !isMinimizedToTitle) {
                    {
                        isMinimizedToTitle = true
                        windowDockable.setFloatingBounds(height = FitContent)
                    }
                } else null,
                onMaximizeAction = if (!isDocked && isMinimizedToTitle) {
                    { isMinimizedToTitle = false }
                } else null,
                onCloseAction = if (isClosable) {
                    {
                        scene.closeWindow(this@BaseWindow, it.ctx)
                    }
                } else null
            )
            if (!isMinimizedToTitle) {
                windowContent()
            }
        }
    }

    protected open fun UiScope.modifyWindow() { }

    protected abstract fun UiScope.windowContent(): Any

    open fun onClose() {
        coroutineContext.cancel()
    }

    protected fun UiScope.applyThemeBackgroundColor() {
        val borderColor = colors.secondaryVariantAlpha(0.3f)
        if (windowDockable.isDocked.use()) {
            modifier
                .background(RectBackground(colors.background))
                .border(RectBorder(borderColor, sizes.borderWidth))
        } else {
            modifier
                .background(RoundRectBackground(colors.background, sizes.gap))
                .border(RoundRectBorder(borderColor, sizes.gap, sizes.borderWidth))
        }
    }
}