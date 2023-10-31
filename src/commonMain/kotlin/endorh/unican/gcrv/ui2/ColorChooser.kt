package endorh.unican.gcrv.ui2

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.AutoPopup
import de.fabmax.kool.util.Color

fun UiScope.ColorChooser(
   hue: MutableStateValue<Float>,
   saturation: MutableStateValue<Float>,
   value: MutableStateValue<Float>,
   alpha: MutableStateValue<Float>? = null,
   hexString: MutableStateValue<String>? = null,
   width: Dp = 450.dp,
   height: Dp = 220.dp,
   scopeName: String? = null,
   onChange: ((Color) -> Unit)? = null,
   mod: UiScope.() -> Unit
) {
   val color = Color.Hsv(hue.use(), saturation.use(), value.use()).toSrgb(a = alpha?.use() ?: 1F)
   val popup = AutoPopup {
      modifier
         .zLayer(this@ColorChooser.uiNode.modifier.zLayer + UiSurface.LAYER_POPUP)
         .background(RoundRectBackground(colors.backgroundVariant, sizes.smallGap))
         .border(RoundRectBorder(colors.primary, sizes.smallGap, sizes.borderWidth))
         .padding(sizes.smallGap)
         .size(width, height)
      ColorChooserH(hue, saturation, value, alpha, hexString, scopeName, onChange)
   }
   Box(Grow.Std, Grow(1F, min = 24.dp)) {
      modifier
         .background(RoundRectBackground(color, 4.dp))
         .margin(2.dp)
         .onClick {
            if (!popup.isVisible.value)
               popup.show(popupPosition(width, height))
         }
      mod()
   }
   popup()
}

fun UiScope.popupPosition(width: Dp, height: Dp): Vec2f {
   val gap = sizes.gap.px
   val y = if (uiNode.topPx < height.px + gap) uiNode.bottomPx + gap else uiNode.topPx - height.px - gap
   val x = if (uiNode.rightPx < width.px) uiNode.leftPx else uiNode.rightPx - width.px
   return Vec2f(x, y)
}