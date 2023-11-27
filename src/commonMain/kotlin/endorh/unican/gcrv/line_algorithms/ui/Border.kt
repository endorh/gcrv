package endorh.unican.gcrv.line_algorithms.ui

import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiRenderer
import de.fabmax.kool.util.Color

open class LeftBorder(val borderColor: Color, val borderWidth: Dp, val inset: Dp = Dp.ZERO) : UiRenderer<UiNode> {
   override fun renderUi(node: UiNode) = with(node) {
      val inPx = inset.px
      getUiPrimitives().localRect(
         inPx, inPx, borderWidth.px, heightPx - 2 * inPx, borderColor)
   }
}