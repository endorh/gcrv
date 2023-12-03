package endorh.unican.gcrv.animation

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.OptionPicker
import endorh.unican.gcrv.renderers.displayName
import endorh.unican.gcrv.ui2.LabeledField
import endorh.unican.gcrv.util.F

fun UiScope.EasingCurvePopupEditor(
   value: Easing, onValueChange: (Easing) -> Unit,
   size: Int = 200, mod: UiScope.() -> Unit
) {
   val editorSize = Dp.fromPx(size.F)
   val popupWidth = editorSize + 200.dp
   val popupHeight = editorSize * 2 + 8.dp
   val easing = remember { mutableStateOf(value) }
   easing.value = value
   val popup = AutoPopup {
      val e = easing.use()
      modifier
         .zLayer(this@EasingCurvePopupEditor.uiNode.modifier.zLayer + UiSurface.LAYER_POPUP)
         .background(RoundRectBackground(colors.backgroundAlpha(0.95F), sizes.smallGap))
         .border(RoundRectBorder(colors.primary, sizes.smallGap, sizes.borderWidth))
         .padding(sizes.smallGap)
         .size(popupWidth, popupHeight)
      Row(Grow.Std, Grow.Std) {
         EasingCurveEditor(e, size) {
            modifier.alignY(AlignmentY.Center)
         }
         Column(Grow.Std, Grow.Std) {
            modifier.margin(4.dp).padding(4.dp)
               .background(RoundRectBackground(colors.backgroundVariant, 4.dp))
            LabeledField("Type", { width(Grow.Std) }) {
               OptionPicker(EasingTypes, e.type, { onValueChange(it.factory()) }) { it() }
            }

            if (e.controlPoints.isNotEmpty()) for (c in e.controlPoints) with(c) {
               editor()
            } else {
               Box(Grow.Std, Grow.Std) {
                  Text("No control points") {
                     modifier.align(AlignmentX.Center, AlignmentY.Center)
                  }
               }
            }
         }
      }
   }

   Row {
      Button("~  ${value.type.displayName()}") {
         modifier
            .textAlignX(AlignmentX.Start)
            .background(RoundRectBackground(Color("BD42BDBD"), 4.dp))
            .width(Grow(1F, max=100.dp))
            .onClick {
               if (!popup.isVisible.value)
                  popup.show(popupPosition(popupWidth, popupHeight))
            }
      }
      // OptionPicker(EasingTypes, value.type, { onValueChange(it.factory()) })
      mod()
   }
   popup()
}

fun UiScope.popupPosition(width: Dp, height: Dp): Vec2f {
   val gap = sizes.gap.px
   val y = if (uiNode.topPx + uiNode.bottomPx < uiNode.surface.viewportHeight.value)
      uiNode.bottomPx + gap
   else uiNode.topPx - height.px - gap
   val x = if (uiNode.leftPx + uiNode.rightPx < uiNode.surface.viewportWidth.value)
      uiNode.leftPx
   else uiNode.rightPx - width.px
   return Vec2f(x, y)
}