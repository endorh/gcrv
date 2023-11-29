package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*

fun UiScope.Group(title: String, expanded: Boolean = true, titleContent: RowScope.() -> Unit = {}, body: UiScope.() -> Unit) {
   val panel = remember { GroupPanel(title, expanded, titleContent, body) }
   panel.setup(title, expanded, titleContent, body)
   panel()
}

open class GroupPanel(
   var title: String,
   var expanded: Boolean,
   var titleContent: RowScope.() -> Unit = {},
   var body: UiScope.() -> Unit
) : Composable {
   val isCollapsed = mutableSerialStateOf(!expanded)
   val isHovered = mutableSerialStateOf(false)

   fun setup(title: String, expanded: Boolean, titleContent: RowScope.() -> Unit, body: UiScope.() -> Unit) {
      this.title = title
      if (this.expanded != expanded) {
         isCollapsed.value = !expanded
         this.expanded = expanded
      }
      this.titleContent = titleContent
      this.body = body
   }

   override fun UiScope.compose() = Column(Grow.Std) {
      modifier.background(RoundRectBackground(colors.backgroundVariant, sizes.smallGap))
      Row(Grow.Std) {
         modifier
            .background(TitleBarBackground(colors.secondaryVariantAlpha(if (isHovered.use()) 0.75F else 0.5F), 8F, isCollapsed.use()))
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

         titleContent()
         Text(title) {}
      }
      if (!isCollapsed.value) content()
   }

   open fun UiScope.content() = body()
}

