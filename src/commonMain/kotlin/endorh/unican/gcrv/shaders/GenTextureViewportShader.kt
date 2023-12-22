package endorh.unican.gcrv.shaders

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import de.fabmax.kool.modules.ui2.UiModifier
import de.fabmax.kool.modules.ui2.UiScope
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.serialization.Vec2i
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.ui2.LabeledField

interface GenTextureViewportShader {
   fun setViewport(left: BigDecimal, top: BigDecimal, right: BigDecimal, bottom: BigDecimal)
   fun setViewport(left: Double, top: Double, right: Double, bottom: Double) {
      setViewport(
         BigDecimal.fromDouble(left), BigDecimal.fromDouble(top),
         BigDecimal.fromDouble(right), BigDecimal.fromDouble(bottom))
   }
}

class NamedProperty<T>(
   val name: String, val getter: () -> T, val setter: (T) -> Unit,
   val editor: PropertyEditor<T>
) {
   fun UiScope.editor(modifier: UiModifier.() -> UiModifier = { this }) {
      LabeledField(name, modifier) {
         with(editor) {
            editor(getter(), setter)
         }
      }
   }
}

interface PropertyEditor<T> {
   fun UiScope.editor(value: T, edit: (T) -> Unit, modifier: UiModifier.() -> UiModifier = { this })
}

object IntPropertyEditor : PropertyEditor<Int> {
   override fun UiScope.editor(value: Int, edit: (Int) -> Unit, modifier: UiModifier.() -> UiModifier) {
      IntField(value, edit) {
         this.modifier.modifier()
      }
   }
}

object FloatPropertyEditor : PropertyEditor<Float> {
   override fun UiScope.editor(value: Float, edit: (Float) -> Unit, modifier: UiModifier.() -> UiModifier) {
      FloatField(value, edit) {
         this.modifier.modifier()
      }
   }
}

object DoublePropertyEditor : PropertyEditor<Double> {
   override fun UiScope.editor(value: Double, edit: (Double) -> Unit, modifier: UiModifier.() -> UiModifier) {
      DoubleField(value, edit) {
         this.modifier.modifier()
      }
   }
}

object Vec2iPropertyEditor : PropertyEditor<Vec2i> {
   override fun UiScope.editor(value: Vec2i, edit: (Vec2i) -> Unit, modifier: UiModifier.() -> UiModifier) {
      Vec2iField(value, edit) {
         this.modifier.modifier()
      }
   }
}

object Vec2fPropertyEditor : PropertyEditor<Vec2f> {
   override fun UiScope.editor(value: Vec2f, edit: (Vec2f) -> Unit, modifier: UiModifier.() -> UiModifier) {
      Vec2fField(value, edit) {
         this.modifier.modifier()
      }
   }
}