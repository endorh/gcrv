package endorh.unican.gcrv.line_algorithms.renderers

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.Point2DRenderer
import endorh.unican.gcrv.line_algorithms.renderers.line.*
import endorh.unican.gcrv.line_algorithms.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.line_algorithms.renderers.point.CirclePointRenderer
import endorh.unican.gcrv.line_algorithms.renderers.point.SquarePointRenderer
import endorh.unican.gcrv.line_algorithms.ui.mix
import endorh.unican.gcrv.util.toTitleCase

val LineRenderers: List<Line2DRenderer> = mutableListOf(
   OrthogonalLineRenderer,
   SlopeOneLineRenderer,
   SlopeInterceptRenderer,
   ExtendedSlopeInterceptRenderer,
   DigitalDifferentialAnalyzerRenderer,
   BresenhamFirstOctantRenderer,
   BresenhamRenderer,
   BresenhamRendererBreadth,
   BresenhamRendererBreadthAntiAlias,
)

val PointRenderers: List<Point2DRenderer> = mutableListOf(
   SquarePointRenderer,
   CirclePointRenderer,
   CircleAntiAliasPointRenderer,
)

fun UiScope.OptionalLineRendererPicker(
   state: MutableStateValue<Line2DRenderer?>, block: ComboBoxScope.() -> Unit = {}
) {
   ComboBox {
      modifier
         .items(listOf("None") + LineRenderers.map { it.name })
         .selectedIndex(if (state.use() != null) LineRenderers.indexOf(state.use()) + 1 else 0)
         .onItemSelected { state.value = if (it > 0) LineRenderers[it - 1] else null }
      block()
   }
}

interface PresentableObject {
   val displayName: String
}

fun Any?.displayName() = when (this) {
   is PresentableObject -> displayName
   null -> "None"
   else -> toString()
}

fun <T: Any> UiScope.OptionPicker(
   values: List<T>, idx: Int, onChange: (Int) -> Unit, modifier: ComboBoxScope.() -> Unit = {},
   scopeName: String? = null, tint: Color? = null
) {
   ComboBox(scopeName) {
      this.modifier
         .items(values.map { it.displayName() })
         .selectedIndex(idx)
         .onItemSelected(onChange).apply {
            textBackgroundColor = colors.secondaryVariantAlpha(0.5f).mix(tint, 0.4F)
         }
      modifier()
   }
}

fun <T> UiScope.OptionalOptionPicker(
   values: List<T>, idx: Int?, onChange: (Int?) -> Unit, modifier: ComboBoxScope.() -> Unit = {},
   scopeName: String? = null, tint: Color? = null
) {
   ComboBox(scopeName) {
      this.modifier
         .items(listOf("None") + values.map { it.displayName() })
         .selectedIndex(if (idx != null) idx + 1 else 0)
         .onItemSelected { onChange(if (it > 0) it - 1 else null) }.apply {
            textBackgroundColor = colors.secondaryVariantAlpha(0.5f).mix(tint, 0.4F)
         }
      modifier()
   }
}

fun UiScope.OptionalPointRendererPicker(
   state: MutableStateValue<Point2DRenderer?>, block: ComboBoxScope.() -> Unit = {}
) {
   ComboBox {
      modifier
         .items(listOf("None") + PointRenderers.map { it.name })
         .selectedIndex(if (state.use() != null) PointRenderers.indexOf(state.use()) + 1 else 0)
         .onItemSelected { state.value = if (it > 0) PointRenderers[it - 1] else null }
      block()
   }
}

fun UiScope.LineRendererPicker(
   state: MutableStateValue<Line2DRenderer>, block: ComboBoxScope.() -> Unit = {}
) {
   ComboBox {
      modifier
         .items(LineRenderers.map { it.name })
         .selectedIndex(LineRenderers.indexOf(state.use()))
         .onItemSelected { state.value = LineRenderers[it] }
      block()
   }
}

fun UiScope.PointRendererPicker(
   state: MutableStateValue<Point2DRenderer>, block: ComboBoxScope.() -> Unit = {}
) {
   ComboBox {
      modifier
         .items(PointRenderers.map { it.name })
         .selectedIndex(PointRenderers.indexOf(state.use()))
         .onItemSelected { state.value = PointRenderers[it] }
      block()
   }
}
