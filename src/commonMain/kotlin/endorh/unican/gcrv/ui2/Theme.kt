package endorh.unican.gcrv.ui2

import de.fabmax.kool.math.clamp
import de.fabmax.kool.util.Color
import kotlin.math.roundToInt

data class Colors(
   val primary: Color,
   val text: Color,
   val primaryText: Color,
   val inactive: Color,
   val background: Color,
   val backgroundOverlay: Color,
   val selection: Color,
   val inactiveSelection: Color,
   val titleBar: Color = primary,
   val inactiveBar: Color = inactive,
   val isLight: Boolean = background.brightness > 0.5F
) {
   private val primaryAlpha = AlphaColorCache(primary)
   private val textAlpha = AlphaColorCache(text)
   private val primaryTextAlpha = AlphaColorCache(primaryText)
   private val inactiveAlpha = AlphaColorCache(inactive)
   private val backgroundAlpha = AlphaColorCache(background)
   private val backgroundOverlayAlpha = AlphaColorCache(backgroundOverlay)
   private val selectionAlpha = AlphaColorCache(selection)
   private val inactiveSelectionAlpha = AlphaColorCache(inactiveSelection)
   private val titleBarAlpha = AlphaColorCache(titleBar)
   private val inactiveBarAlpha = AlphaColorCache(inactiveBar)

   fun primaryAlpha(alpha: Float) = primaryAlpha.getAlphaColor(alpha)
   fun textAlpha(alpha: Float) = textAlpha.getAlphaColor(alpha)
   fun primaryTextAlpha(alpha: Float) = primaryTextAlpha.getAlphaColor(alpha)
   fun inactiveAlpha(alpha: Float) = inactiveAlpha.getAlphaColor(alpha)
   fun backgroundAlpha(alpha: Float) = backgroundAlpha.getAlphaColor(alpha)
   fun backgroundOverlayAlpha(alpha: Float) = backgroundOverlayAlpha.getAlphaColor(alpha)
   fun selectionAlpha(alpha: Float) = selectionAlpha.getAlphaColor(alpha)
   fun inactiveSelectionAlpha(alpha: Float) = inactiveSelectionAlpha.getAlphaColor(alpha)
   fun titleBarAlpha(alpha: Float) = titleBarAlpha.getAlphaColor(alpha)
   fun inactiveBarAlpha(alpha: Float) = inactiveBarAlpha.getAlphaColor(alpha)

   companion object {
      fun darkColors(
         primary: Color = Color("0380FFFF"),
         text: Color = Color("E0E0E0FF"),
         primaryText: Color = Color("FFFFFFFF"),
         inactive: Color = Color("646464FF"),
         background: Color = Color("121212FF"),
         backgroundOverlay: Color = Color("A0A0A042"),
         selection: Color = primary.withAlpha(0.3F),
         inactiveSelection: Color = inactive.withAlpha(0.3F),
         titleBar: Color = primary,
         inactiveBar: Color = inactive
      ): Colors = Colors(
         primary, text, primaryText, inactive,
         background, backgroundOverlay,
         selection, inactiveSelection,
         titleBar, inactiveBar, false
      )

      fun lightColors(
         primary: Color = Color("24BDEEFF"),
         text: Color = Color("000000FF"),
         primaryText: Color = Color("FFFFFFFF"),
         inactive: Color = Color("646464FF"),
         background: Color = Color("E0E0E0FF"),
         backgroundOverlay: Color = Color("80808042"),
         selection: Color = primary.withAlpha(0.3F),
         inactiveSelection: Color = inactive.withAlpha(0.3F),
         titleBar: Color = primary,
         inactiveBar: Color = inactive
      ): Colors = Colors(
         primary, text, primaryText, inactive,
         background, backgroundOverlay,
         selection, inactiveSelection,
         titleBar, inactiveBar, true
      )
   }

   private class AlphaColorCache(baseColor: Color) {
      val alphaColors = Array<Color>(20) { baseColor.withAlpha(it / 20f) }

      fun getAlphaColor(alpha: Float): Color {
         val alphaI = (alpha * 20f).roundToInt().clamp(0, alphaColors.lastIndex)
         return alphaColors[alphaI]
      }
   }
}
