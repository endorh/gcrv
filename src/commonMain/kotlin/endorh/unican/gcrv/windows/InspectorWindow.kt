package endorh.unican.gcrv.windows

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.line_algorithms.ui.LeftBorder
import endorh.unican.gcrv.objects.AnimProperty
import endorh.unican.gcrv.objects.CompoundAnimProperty
import endorh.unican.gcrv.objects.PropertyNode
import endorh.unican.gcrv.transformations.TransformProperty
import endorh.unican.gcrv.ui2.Section
import endorh.unican.gcrv.util.toTitleCase

class InspectorWindow(scene: EditorScene) : BaseWindow("Inspector", scene, true) {

    init {
        windowDockable.setFloatingBounds(width = Dp(150F), height = Dp(400F))
    }

    override fun UiScope.windowContent() = ScrollArea {
        modifier.width(Grow.Std)
        Column(Grow.Std) {
            val properties = scene.selectedObjects.use()
                .flatMap { it.properties.values }
                .groupBy { it.name }
                .values.sortedBy { it.first() is CompoundAnimProperty }
            // println("Selected object: ${scene.selectedObjects.firstOrNull()?.name}")
            // println("Properties: $properties")
            // println("All properties: ${scene.selectedObjects.use().flatMap { it.properties.allProperties }}")
            for (propSet in properties)
                propertyNodeEditor(propSet)
        }
    }

    private fun ColumnScope.propertyNodeEditor(nodeSet: List<PropertyNode>) {
        when (val first = nodeSet.first()) {
            is AnimProperty<*> -> propertyEditor(nodeSet.filterIsInstance<AnimProperty<*>>().filter { it::class == first::class })
            is CompoundAnimProperty -> compoundPropertyEditor(nodeSet.filterIsInstance<CompoundAnimProperty>().filter { it::class == first::class })
        }
    }
    private fun ColumnScope.propertyEditor(propSet: List<AnimProperty<*>>) {
        @Suppress("UNCHECKED_CAST")
        propSet as List<AnimProperty<Any>>
        val first = propSet.first()
        if (first.isUnique && propSet.size > 1) return
        Row(Grow.Std) {
            modifier.margin(2.dp).padding(4.dp)
            if (scene.selectedProperties.use().firstOrNull() == first)
                modifier.border(RoundRectBorder(Color("FF42FF80"), 4.dp, 2.dp))
            with(first) {
                editor(propSet, onFocus = {
                    scene.selectedProperties.value = if (scene.selectedProperties.value.firstOrNull() == first)
                        emptyList() else propSet
                })
            }
        }
    }
    private fun ColumnScope.compoundPropertyEditor(propSet: List<CompoundAnimProperty>) {
        val first = propSet.first()
        val subProps = first.properties.values
            .sortedBy { it is CompoundAnimProperty }
            .map { p -> propSet.mapNotNull { it.properties[p.name] } }
        if (subProps.isNotEmpty()) Section(first.name.toTitleCase(), first.showExpanded) {
            Column(Grow.Std) {
                this.modifier.padding(start=8.dp).border(LeftBorder(colors.secondaryVariant, 2.dp, 4.dp))
                for (prop in subProps)
                    propertyNodeEditor(prop)
            }
        }
    }
}
