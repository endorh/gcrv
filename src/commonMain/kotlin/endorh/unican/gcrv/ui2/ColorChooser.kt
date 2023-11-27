package endorh.unican.gcrv.ui2

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.AutoPopup
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.ui.BlendTextField

private val misPositionedColorMarkPattern = Regex("""(?<=.)#""")
fun String.onlyColorCharacters(): String =
   filter { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' || it == '#' }
      .replace(misPositionedColorMarkPattern, "")
      .uppercase()

fun UiScope.ColorChooser(
   hue: MutableStateValue<Float>,
   saturation: MutableStateValue<Float>,
   value: MutableStateValue<Float>,
   alpha: MutableStateValue<Float>?,
   hexString: MutableStateValue<String>,
   textFieldTint: Color? = null,
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
         .background(RoundRectBackground(colors.backgroundAlpha(0.95F), sizes.smallGap))
         .border(RoundRectBorder(colors.primary, sizes.smallGap, sizes.borderWidth))
         .padding(sizes.smallGap)
         .size(width, height)
      ColorChooserH(hue, saturation, value, alpha, hexString, "$scopeName/chooser", onChange)
   }
   Row(Grow.Std, scopeName="$scopeName/row") {
      Box(24.dp, 24.dp) {
         modifier
            .background(RoundRectBackground(color, 4.dp))
            .margin(2.dp)
            .onClick {
               if (!popup.isVisible.value)
                  popup.show(popupPosition(width, height))
            }
      }
      BlendTextField(hexString.use(), "$scopeName/hex", textFieldTint) {
         modifier.padding(start=4.dp).width(Grow.Std).onChange {
            val s = it.onlyColorCharacters()
            hexString.value = s
            Color.fromHexOrNull(s)?.let { color ->
               val hsv = color.toHsv()
               hue.value = hsv.h
               saturation.value = hsv.s
               value.value = hsv.v
               alpha?.value = color.a
               onChange?.invoke(color)
            }
         }
      }
      mod()
   }
   popup()
}

fun UiScope.ColorField(
   value: Color, onChange: (Color) -> Unit,
   allowAlpha: Boolean = true,
   scopeName: String? = null,
   textFieldTint: Color? = null,
   width: Dp = 450.dp, height: Dp = 220.dp,
   mod: UiScope.() -> Unit
) {
   Row(Grow.Std, scopeName=scopeName) {
      val hsv = value.toHsv()
      val hue = remember { mutableStateOf(hsv.h) }
      val sat = remember { mutableStateOf(hsv.s) }
      val `val` = remember { mutableStateOf(hsv.v) }
      val alpha = remember { mutableStateOf(value.a) }
      val hexString = remember { mutableStateOf(value.toHexString(allowAlpha).uppercase()) }
      ColorChooser(
         hue, sat, `val`, if (allowAlpha) alpha else null, hexString, textFieldTint, width, height,
         "chooser", onChange
      ) {

      }
      mod()
   }
}

fun UiScope.popupPosition(width: Dp, height: Dp): Vec2f {
   val gap = sizes.gap.px
   val y = if (uiNode.topPx < height.px + gap) uiNode.bottomPx + gap else uiNode.topPx - height.px - gap
   val x = if (uiNode.rightPx < width.px) uiNode.leftPx else uiNode.rightPx - width.px
   return Vec2f(x, y)
}