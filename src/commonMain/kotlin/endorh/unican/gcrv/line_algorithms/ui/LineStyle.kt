package endorh.unican.gcrv.line_algorithms.ui

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.LineStyle
import endorh.unican.gcrv.line_algorithms.ObjectProperty
import endorh.unican.gcrv.line_algorithms.renderers.LineRendererPicker
import endorh.unican.gcrv.line_algorithms.renderers.OptionalLineRendererPicker
import endorh.unican.gcrv.ui2.ColorChooser
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I

class LineStyleProperty(value: LineStyle) : ObjectProperty<LineStyle>(value) {
   override val isSimpleEditor = false
   override fun UiScope.editor(modifier: UiScope.() -> Unit) {
      LineStyleEditor(this@LineStyleProperty)
   }
}

fun UiScope.LineStyleEditor(state: MutableStateValue<LineStyle>) {
   LabeledField("Width") {
      IntField(state.value.breadth.I, { state.value = state.value.copy(breadth = it.F) }) { it() }
   }
   val rendererState = remember { mutableStateOf(state.value.renderer) }
      .onChange { state.value = state.value.copy(renderer = it) }
   LabeledField("Renderer") { OptionalLineRendererPicker(rendererState) { it() } }

   LabeledColorField("Color", state.value.color, {
      state.value = state.value.copy(color = it)
   })
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