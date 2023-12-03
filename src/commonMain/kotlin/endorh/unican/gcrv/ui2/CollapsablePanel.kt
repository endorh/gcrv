package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*

fun UiScope.FixedSection(title: String, body: UiScope.() -> Unit) {
   val panel = remember { CollapsablePanel(title, true, false, body) }
   panel.setup(title, true, false, body)
   panel()
}

fun UiScope.Section(title: String, expanded: Boolean = true, body: UiScope.() -> Unit) {
   val panel = remember { CollapsablePanel(title, expanded, true, body) }
   panel.setup(title, expanded, true, body)
   panel()
}

open class CollapsablePanel(
   var title: String,
   var expanded: Boolean,
   var collapsible: Boolean = true,
   var body: UiScope.() -> Unit
) : Composable {
   val isCollapsed = mutableSerialStateOf(!expanded)
   val isHovered = mutableSerialStateOf(false)

   fun setup(title: String, expanded: Boolean, collapsible: Boolean = true, body: UiScope.() -> Unit) {
      this.title = title
      val ex = expanded || !collapsible
      if (this.expanded != ex) {
         isCollapsed.value = !ex
         this.expanded = ex
      }
      this.collapsible = collapsible
      this.body = body
   }

   override fun UiScope.compose() = Column(Grow.Std) {
      modifier.backgroundColor(colors.backgroundVariant)
      Row(Grow.Std) {
         modifier
            .backgroundColor(colors.secondaryVariantAlpha(if (isHovered.use()) 0.75F else 0.5F))
            .padding(horizontal = sizes.gap, vertical = sizes.smallGap)
         if (collapsible) {
            modifier
               .onClick { isCollapsed.toggle() }
               .onEnter { isHovered.set(true) }
               .onExit { isHovered.set(false) }

            Arrow(if (isCollapsed.use()) 0F else 90F) {
               modifier
                  .size(sizes.gap * 1.5F, sizes.gap * 1.5F)
                  .margin(horizontal = sizes.gap)
                  .alignY(AlignmentY.Center)
            }
         }

         Text(title) { }
      }
      if (!isCollapsed.value) {
         Column(Grow.Std) {
            content()
         }
      } else {
         divider(colors.secondaryVariantAlpha(0.75F), horizontalMargin = 0.dp, thickness = 1.dp)
      }
   }

   open fun UiScope.content() = body()
}
