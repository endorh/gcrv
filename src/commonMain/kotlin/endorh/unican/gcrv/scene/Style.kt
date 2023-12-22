package endorh.unican.gcrv.scene

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.ui2.IntField
import endorh.unican.gcrv.ui2.LabeledColorField
import endorh.unican.gcrv.ui2.LabeledField
import endorh.unican.gcrv.scene.property.*
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.I
import kotlin.reflect.KProperty

class LineStyleProperty(
   lineStyle: LineStyle = LineStyle(),
   start: PointStyle = PointStyle(),
   end: PointStyle = PointStyle()
) : CompoundAnimProperty() {
   var color by color(lineStyle.color)
   var breadth by float(lineStyle.breadth)
   var renderer by nullOption(LineRenderers, lineStyle.renderer)

   val start by pointStyle(start)
   val end by pointStyle(end)

   override val showExpanded: Boolean get() = false
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   var lineStyle
      get() = LineStyle(color, breadth, renderer)
      set(value) {
         color = value.color
         breadth = value.breadth
         renderer = value.renderer
      }
}

class PointStyleProperty(value: PointStyle = PointStyle()) : CompoundAnimProperty() {
   var color by color(value.color)
   var size by float(value.size)
   var renderer by nullOption(PointRenderers, value.renderer)

   override val showExpanded: Boolean get() = false
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   var pointStyle
      get() = PointStyle(color, size, renderer)
      set(value) {
         color = value.color
         size = value.size
         renderer = value.renderer
      }
}

class CubicSplineStyleProperty(
   style: CubicSplineStyle = CubicSplineStyle(),
   polygon: LineStyle = LineStyle(Color.GRAY),
   start: PointStyle = PointStyle(),
   mid: PointStyle = PointStyle(),
   end: PointStyle = PointStyle()
) : CompoundAnimProperty() {
   var color by color(style.color)
   var breadth by float(style.breadth)
   var renderer by nullOption(CubicSplineRenderers, style.renderer)

   var polygonColor by color(polygon.color)
   var polygonBreadth by float(polygon.breadth)
   val start by pointStyle(start)
   val mid by pointStyle(mid)
   val end by pointStyle(end)

   override val showExpanded: Boolean get() = false
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   var cubicSplineStyle
      get() = CubicSplineStyle(color, breadth, renderer)
      set(value) {
         color = value.color
         breadth = value.breadth
         renderer = value.renderer
      }
   var startStyle by this.start::pointStyle
   var midStyle by this.mid::pointStyle
   var endStyle by this.end::pointStyle
   var polygonStyle: LineStyle
      get() = LineStyle(polygonColor, polygonBreadth)
      set(value) {
         polygonColor = value.color
         polygonBreadth = value.breadth
      }
}

fun UiScope.LineStyleEditor(value: LineStyle, onChange: (LineStyle) -> Unit = {}, modifier: UiScope.() -> Unit = {}) = Column {
   modifier()
   LabeledField("Width") {
      IntField(value.breadth.I, { onChange(value.copy(breadth = it.F)) }) { it() }
   }
   LabeledField("Renderer") { OptionalOptionPicker(LineRenderers, value.renderer, { onChange(value.copy(renderer = it)) }) { it() } }
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
   LabeledField("Renderer") { OptionalOptionPicker(PointRenderers, value.renderer, { onChange(value.copy(renderer = it)) }) { it() } }
   LabeledColorField("Color", value.color, { onChange(value.copy(color = it)) })
}
fun UiScope.PointStyleEditor(state: MutableStateValue<PointStyle>, modifier: UiScope.() -> Unit = {}) {
   PointStyleEditor(state.value, { state.value = it }, modifier)
}

fun UiScope.CubicSplineStyleEditor(
   value: CubicSplineStyle, onChange: (CubicSplineStyle) -> Unit = {}, modifier: UiScope.() -> Unit = {}
) {
   modifier()
   LabeledField("Width") {
      IntField(value.breadth.I, { onChange(value.copy(breadth = it.F)) }) { it() }
   }
   LabeledField("Renderer") { OptionalOptionPicker(CubicSplineRenderers, value.renderer, { onChange(value.copy(renderer = it)) }) { it() } }
   LabeledColorField("Color", value.color, { onChange(value.copy(color = it)) })
}
fun UiScope.CubicSplineStyleEditor(state: MutableStateValue<CubicSplineStyle>, modifier: UiScope.() -> Unit = {}) {
   CubicSplineStyleEditor(state.value, { state.value = it }, modifier)
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