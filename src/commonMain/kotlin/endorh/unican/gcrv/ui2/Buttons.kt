package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*

fun UiScope.SmallButton(
   icon: String,
   modifier: UiModifier.() -> UiModifier = { this },
   scopeName: String? = "button/$icon",
   onClick: (PointerEvent) -> Unit
) {
   Button(icon, scopeName) {
      this.modifier
         .size(sizes.gap * 2F, sizes.gap * 2F)
         .margin(horizontal = sizes.smallGap)
         .alignY(AlignmentY.Center).onClick {
            onClick(it)
         }
      this.modifier.modifier()
   }
}