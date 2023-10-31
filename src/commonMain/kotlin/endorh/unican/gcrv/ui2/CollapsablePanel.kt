package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*

fun UiScope.Section(title: String, expanded: Boolean = true, body: UiScope.() -> Unit) {
   val panel = remember { CollapsablePanel(title, expanded, body) }
   panel()
}

open class CollapsablePanel(val title: String, expanded: Boolean, val body: UiScope.() -> Unit) : Composable {
   val isCollapsed = mutableStateOf(!expanded)
   val isHovered = mutableStateOf(false)

   override fun UiScope.compose() = Column(Grow.Std) {
      modifier.backgroundColor(colors.backgroundVariant)
      Row(Grow.Std) {
         modifier
            .backgroundColor(colors.secondaryVariantAlpha(if (isHovered.use()) 0.75F else 0.5F))
            .onClick { isCollapsed.toggle() }
            .padding(horizontal = sizes.gap, vertical = sizes.smallGap)
            .onEnter { isHovered.set(true) }
            .onExit { isHovered.set(false) }

         Arrow(if (isCollapsed.use()) 0F else 90F) {
            modifier
               .size(sizes.gap * 1.5F, sizes.gap * 1.5F)
               .margin(horizontal = sizes.gap)
               .alignY(AlignmentY.Center)
         }
         Text(title) { }
      }
      if (!isCollapsed.value) {
         content()
      } else {
         divider(colors.secondaryVariantAlpha(0.75F), horizontalMargin = 0.dp, thickness = 1.dp)
      }
   }

   open fun UiScope.content() = body()
}
