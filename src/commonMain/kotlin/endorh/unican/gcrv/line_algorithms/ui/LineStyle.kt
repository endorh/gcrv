package endorh.unican.gcrv.line_algorithms.ui

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.line_algorithms.LineStyle
import endorh.unican.gcrv.line_algorithms.PointStyle
import endorh.unican.gcrv.line_algorithms.renderers.LineRenderers
import endorh.unican.gcrv.line_algorithms.renderers.OptionalLineRendererPicker
import endorh.unican.gcrv.line_algorithms.renderers.OptionalPointRendererPicker
import endorh.unican.gcrv.line_algorithms.renderers.PointRenderers
import endorh.unican.gcrv.objects.*
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import kotlin.reflect.KProperty

class LineStyleProperty(lineStyle: LineStyle) : CompoundAnimProperty() {
   var color by color(lineStyle.color)
   var breadth by float(lineStyle.breadth)
   var renderer by option(LineRenderers, lineStyle.renderer)

   override val showExpanded: Boolean get() = false
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   val lineStyle get() = LineStyle(color, breadth, renderer)
}

class PointStyleProperty(value: PointStyle) : CompoundAnimProperty() {
   var color by color(value.color)
   var size by float(value.size)
   var renderer by option(PointRenderers, value.renderer)

   override val showExpanded: Boolean get() = false
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   val pointStyle get() = PointStyle(color, size, renderer)
}

fun UiScope.LineStyleEditor(value: LineStyle, onChange: (LineStyle) -> Unit = {}, modifier: UiScope.() -> Unit = {}) = Column {
   modifier()
   LabeledField("Width") {
      IntField(value.breadth.I, { onChange(value.copy(breadth = it.F)) }) { it() }
   }
   val rendererState = remember { mutableStateOf(value.renderer) }
      .onChange { onChange(value.copy(renderer = it)) }
   LabeledField("Renderer") { OptionalLineRendererPicker(rendererState) { it() } }

   LabeledColorField("Color", value.color, { onChange(value.copy(color = it)) })
}
fun UiScope.LineStyleEditor(state: MutableStateValue<LineStyle>, modifier: UiScope.() -> Unit = {}) {
   LineStyleEditor(state.value, { state.value = it }, modifier)
}

fun UiScope.PointStyleEditor(value: PointStyle, onChange: (PointStyle) -> Unit = {}, modifier: UiScope.() -> Unit = {}) = Column {
   modifier()
   LabeledField("Size") {
      IntField(value.size.I, { onChange(value.copy(size = it.F)) }) { it() }
   }
   val rendererState = remember { mutableStateOf(value.renderer) }
      .onChange { onChange(value.copy(renderer = it)) }
   LabeledField("Renderer") { OptionalPointRendererPicker(rendererState) { it() } }

   LabeledColorField("Color", value.color, { onChange(value.copy(color = it)) })
}
fun UiScope.PointStyleEditor(state: MutableStateValue<PointStyle>, modifier: UiScope.() -> Unit = {}) {
   PointStyleEditor(state.value, { state.value = it }, modifier)
}

private fun UiScope.SliderField(
   label: String,
   value: Float,
   onChange: (Float) -> Unit,
   min: Float = 0F, max: Float = 1F
) {
   Row {
      Text(label) {
         modifier
            .alignX(AlignmentX.Start)
            .alignY(AlignmentY.Center)
      }
      Slider(value, min, max, "Slider:$label") {
         modifier
            .alignX(AlignmentX.End)
            .alignY(AlignmentY.Center)
            .width(Grow.Std)
            .margin(horizontal = sizes.gap)
            .onChange { onChange(it) }
      }
   }
}