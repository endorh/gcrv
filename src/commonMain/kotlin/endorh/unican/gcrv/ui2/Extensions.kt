package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.ButtonScope
import de.fabmax.kool.modules.ui2.PointerEvent
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.util.Color

fun ButtonScope.onClick(action: (PointerEvent) -> Unit) {
    modifier.onClick(action)
}

private val TRANSPARENT_COLOR = Color(0f, 0f, 0f, 0f)
val Color.Companion.TRANSPARENT get() = TRANSPARENT_COLOR
