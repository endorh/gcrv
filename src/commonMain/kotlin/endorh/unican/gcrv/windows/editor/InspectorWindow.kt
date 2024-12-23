package endorh.unican.gcrv.windows.editor

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.ui2.LeftBorder
import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.scene.property.CompoundAnimProperty
import endorh.unican.gcrv.scene.property.PropertyList
import endorh.unican.gcrv.scene.property.PropertyNode
import endorh.unican.gcrv.ui2.Group
import endorh.unican.gcrv.ui2.SmallButton
import endorh.unican.gcrv.util.toTitleCase
import endorh.unican.gcrv.windows.BaseWindow

class InspectorWindow(scene: EditorScene) : BaseWindow<EditorScene>("Inspector", scene, true) {

    init {
        windowDockable.setFloatingBounds(width = Dp(150F), height = Dp(400F))
    }

    override fun UiScope.windowContent() = ScrollArea {
        modifier.width(Grow.Std)
        Column(Grow.Std) {
            val properties = scene.selectedObjects.use()
                .flatMap { it.properties.values }
                .groupBy { it.name }
                .values.sortedByDescending { it.first().priority }
            for (propSet in properties)
                propertyNodeEditor(propSet)
        }
    }

    private fun ColumnScope.propertyNodeEditor(nodeSet: List<PropertyNode<*>>) {
        when (val first = nodeSet.first()) {
            is AnimProperty<*> -> propertyEditor(nodeSet.filterIsInstance<AnimProperty<*>>().filter { it::class == first::class })
            is CompoundAnimProperty -> compoundPropertyEditor(nodeSet.filterIsInstance<CompoundAnimProperty>().filter { it::class == first::class })
            is PropertyList<*, *> -> propertyListEditor(nodeSet.filterIsInstance<PropertyList<*, *>>().filter { it::class == first::class })
        }
    }
    private fun ColumnScope.propertyEditor(propSet: List<AnimProperty<*>>) {
        @Suppress("UNCHECKED_CAST")
        propSet as List<AnimProperty<Any>>
        val first = propSet.first()
        if (first.isInternal || first.isUnique && propSet.size > 1) return
        Row(Grow.Std) {
            modifier.margin(horizontal=2.dp, vertical=1.dp).padding(horizontal=4.dp)
            with(first) {
                editor(propSet)
            }
        }
    }
    private fun ColumnScope.compoundPropertyEditor(propSet: List<CompoundAnimProperty>) {
        val first = propSet.first()
        val subProps = first.properties.values
            .sortedByDescending { it.priority }
            .map { p -> propSet.mapNotNull { it.properties[p.name] } }
        if (subProps.isNotEmpty()) Group(first.name.toTitleCase(), first.showExpanded) {
            Column(Grow.Std) {
                modifier.padding(start=8.dp).border(LeftBorder(colors.secondaryVariant, 2.dp))
                for (prop in subProps)
                    propertyNodeEditor(prop)
            }
        }
    }
    private fun ColumnScope.propertyListEditor(propSet: List<PropertyList<*, *>>) {
        val first = propSet.first()
        val lists = propSet.filter { it.size == first.size }
        Group(first.name.toTitleCase(), first.showExpanded, titleContent = {
            SmallButton("+", { margin(end=0.dp) }) {
                for (list in lists)
                    list.insert()
            }
            SmallButton("-", { margin(start=0.dp) }) {
                for (list in lists)
                    list.remove()
            }
        }) {
            Column(Grow.Std) {
                modifier.padding(start = 8.dp).border(LeftBorder(colors.secondaryVariant, 2.dp))
                for (i in 0..<first.size)
                    propertyNodeEditor(lists.map { it[i] })
            }
        }
    }
}
