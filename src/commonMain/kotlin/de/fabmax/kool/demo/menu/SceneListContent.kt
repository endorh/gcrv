package de.fabmax.kool.demo.menu

import de.fabmax.kool.demo.Scenes
import de.fabmax.kool.demo.Settings
import de.fabmax.kool.demo.UiSizes
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MsdfFont

class SceneListContent(val menu: SceneMenu) : Composable {
    private val nonHiddenDemoItems = mutableListOf<DemoItem>()
    private val allDemoItems = mutableListOf<DemoItem>()

    init {
        Scenes.categories.forEach { cat ->
            val titleItem = DemoItem(cat.title, cat.getCategoryColor(0f), true, cat, null)
            allDemoItems += titleItem
            if (!cat.isHidden) {
                nonHiddenDemoItems += titleItem
            }

            cat.entries.forEach { entry ->
                val demoItem = DemoItem(
                    entry.title,
                    entry.color,
                    false,
                    cat,
                    entry
                )
                allDemoItems += demoItem
                if (!cat.isHidden) {
                    nonHiddenDemoItems += demoItem
                }
            }
        }
    }

    override fun UiScope.compose() = Column {
        modifier
            .height(Grow.Std)
            .width(Grow.Std)

        LazyList(
            containerModifier = { it.background(null) },
            vScrollbarModifier = { it.colors(color = colors.onBackgroundAlpha(0.2f), hoverColor = colors.onBackgroundAlpha(0.4f)) }
        ) {
            var hoveredIndex by remember(-1)
            val demoItems = if (Settings.showHiddenDemos.use()) allDemoItems else nonHiddenDemoItems
            itemsIndexed(demoItems) { i, item ->
                Text(item.text) {
                    modifier
                        .width(Grow.Std)
                        .height(UiSizes.baseSize)
                        .padding(horizontal = sizes.gap * 1.25f)
                        .textAlignY(AlignmentY.Center)
                        .onEnter { hoveredIndex = i }
                        .onExit { hoveredIndex = -1 }
                        .onClick {
                            if (!item.isTitle) {
                                item.demo?.let { menu.sceneLoader.loadScene(it) }
                                menu.isExpanded = false
                            }
                        }

                    if (item.isTitle) {
                        categoryTitleStyle(item)
                    } else {
                        demoEntryStyle(item, hoveredIndex == i)
                    }
                }
            }
        }
    }

    private fun TextScope.demoEntryStyle(item: DemoItem, isHovered: Boolean) {
        if (isHovered) {
            modifier
                .backgroundColor(item.color)
                .textColor(Color.WHITE)
        } else if (item.demo?.id == Settings.selectedScene.value) {
            modifier
                .backgroundColor(item.color.withAlpha(0.2f))
                .textColor(Color.WHITE)
        } else {
            modifier
                .textColor(item.color.mix(Color.WHITE, 0.5f))
        }
    }

    private fun TextScope.categoryTitleStyle(item: DemoItem) {
        modifier
            .background(TitleBgRenderer(SceneMenu.titleBgMesh, item.category.fromColor, item.category.toColor))
            .textColor(Color.WHITE)
            .font((sizes.largeText as MsdfFont).copy(glowColor = SceneMenu.titleTextGlowColor))
    }

    private class DemoItem(val text: String, val color: Color, val isTitle: Boolean, val category: Scenes.Category, val demo: Scenes.Entry?)
}