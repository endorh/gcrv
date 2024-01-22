package endorh.unican.gcrv.renderers

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.fill.poly.ConvexTestFillRenderer
import endorh.unican.gcrv.renderers.fill.poly.NoOpFillRenderer
import endorh.unican.gcrv.scene.CubicSpline2DRenderer
import endorh.unican.gcrv.scene.Line2DRenderer
import endorh.unican.gcrv.scene.Point2DRenderer
import endorh.unican.gcrv.renderers.line.*
import endorh.unican.gcrv.renderers.point.*
import endorh.unican.gcrv.renderers.spline.DebugSplineRenderer
import endorh.unican.gcrv.renderers.spline.VariableInterpolationSplineRenderer
import endorh.unican.gcrv.scene.PolyFill2DRenderer
import endorh.unican.gcrv.ui2.mix

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
   TiltedSquarePointRenderer,
   CirclePointRenderer,
   CircleAntiAliasPointRenderer,
   HollowCircleAntiAliasPointRenderer,
)

val CubicSplineRenderers: List<CubicSpline2DRenderer> = mutableListOf(
   DebugSplineRenderer,
   VariableInterpolationSplineRenderer,
)

val PolyFillRenderers: List<PolyFill2DRenderer> = mutableListOf(
   NoOpFillRenderer,
   ConvexTestFillRenderer
)

interface PresentableObject {
   val displayName: String
}

fun Any?.displayName() = when (this) {
   is PresentableObject -> displayName
   null -> "None"
   else -> toString()
}

fun <T: Any> UiScope.OptionIdxPicker(
   values: List<T>, idx: Int, onChange: (Int) -> Unit, scopeName: String? = null,
   tint: Color? = null, modifier: ComboBoxScope.() -> Unit = {}
) {
   ComboBox(scopeName) {
      this.modifier
         .items(values.map { it.displayName() })
         .selectedIndex(idx)
         .onItemSelected(onChange).apply {
            textBackgroundColor = colors.secondaryVariantAlpha(0.5f).mix(tint, 0.4F)
         }.apply {
            popupBackgroundColor = colors.backgroundAlpha(0.95F)
         }
      modifier()
   }
}

fun <T: Any> UiScope.OptionPicker(
   values: List<T>, value: T, onChange: (T) -> Unit, scopeName: String? = null,
   tint: Color? = null, modifier: ComboBoxScope.() -> Unit = {}
) = OptionIdxPicker(
   values, values.indexOf(value), { onChange(values[it]) }, scopeName, tint, modifier
)

fun <T> UiScope.OptionalOptionIdxPicker(
   values: List<T>, idx: Int?, onChange: (Int?) -> Unit, scopeName: String? = null,
   tint: Color? = null, modifier: ComboBoxScope.() -> Unit = {}
) {
   ComboBox(scopeName) {
      this.modifier
         .items(listOf("None") + values.map { it.displayName() })
         .selectedIndex(if (idx != null) idx + 1 else 0)
         .onItemSelected { onChange(if (it > 0) it - 1 else null) }.apply {
            textBackgroundColor = colors.secondaryVariantAlpha(0.5f).mix(tint, 0.4F)
         }.apply {
            popupBackgroundColor = colors.backgroundAlpha(0.95F)
         }
      modifier()
   }
}

fun <T> UiScope.OptionalOptionPicker(
   values: List<T>, value: T?, onChange: (T?) -> Unit, scopeName: String? = null,
   tint: Color? = null, modifier: ComboBoxScope.() -> Unit = {}
) = OptionalOptionIdxPicker<T>(
   values, if (value != null) values.indexOf(value) else null, { onChange(if (it != null) values[it] else null) },
   scopeName, tint, modifier
)
