package endorh.unican.gcrv.line_algorithms.ui

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.ui2.ColorChooser

fun String.onlyNumberChars() = filter { it.isDigit() || it in "+- " }
fun String.onlyDecimalNumberChars() = filter { it.isDigit() || it in "+-.eE" }

fun UiScope.IntField(
   value: Int, onValueChange: (Int) -> Unit = {}, scopeName: String? = null,
   block: TextFieldScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   TextField(invalidStringState.value ?: value.toString(), scopeName) {
      modifier.onChange { s ->
         val sanitized = s.onlyNumberChars()
         sanitized.toIntOrNull()?.let {
            onValueChange(it)
            invalidStringState.value = null
         } ?: run { invalidStringState.value = sanitized }
      }
      block()
   }
}

fun UiScope.FloatField(
   value: Float, onValueChange: (Float) -> Unit = {}, scopeName: String? = null,
   block: TextFieldScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   TextField(invalidStringState.value ?: value.toString(), scopeName) {
      modifier.onChange { s ->
         val sanitized = s.onlyDecimalNumberChars()
         sanitized.toFloatOrNull()?.let {
            onValueChange(it)
            invalidStringState.value = null
         } ?: run { invalidStringState.value = sanitized }
      }
      block()
   }
}

fun UiScope.DoubleField(
   value: Double, onValueChange: (Double) -> Unit = {}, scopeName: String? = null,
   block: TextFieldScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   TextField(invalidStringState.value ?: value.toString(), scopeName) {
      modifier.onChange { s ->
         val sanitized = s.onlyDecimalNumberChars()
         sanitized.toDoubleOrNull()?.let {
            onValueChange(it)
            invalidStringState.value = null
         } ?: run { invalidStringState.value = sanitized }
      }
      block()
   }
}

fun UiScope.Vec2iField(
   value: Vec2i, onValueChange: (Vec2i) -> Unit = {}, scopeName: String? = null,
   rowModifier: (UiModifier) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   Row {
      rowModifier(modifier)
      IntField(value.x, { onValueChange(Vec2i(it, value.y)) }, scopeName?.let { "$it/x" }) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(end = 4.dp)
      }
      IntField(value.y, { onValueChange(Vec2i(value.x, it)) }, scopeName?.let { "$it/y" }) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(start = 4.dp)
      }
      block()
   }
}

internal fun UiScope.LabeledField(
   label: String, field: UiScope.(UiScope.() -> Unit) -> Unit
) {
   Row(scopeName = "Field/$label") {
      modifier.margin(2.dp).padding(4.dp).backgroundColor(colors.backgroundVariant).width(Grow.Std)
      Text(label) {
         modifier
            .margin(end = 8.dp)
            .width(Grow(0.3F, min = 40.dp))
            .alignY(AlignmentY.Center)
      }
      field {
         modifier.width(Grow(0.7F, min = 60.dp))
      }
   }
}

fun UiScope.LabeledStringField(
   label: String, value: String, onValueChange: (String) -> Unit = {}, block: TextFieldScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      TextField(value) { modifier.onChange { onValueChange(it) }; mod(); block() }
   }
}
fun UiScope.LabeledStringField(label: String, value: MutableStateValue<String>, block: TextFieldScope.() -> Unit = {}) =
   LabeledStringField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledIntField(
   label: String, value: Int, onValueChange: (Int) -> Unit = {}, block: TextFieldScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      IntField(value, onValueChange) { mod(); block() }
   }
}
fun UiScope.LabeledIntField(label: String, value: MutableStateValue<Int>, block: TextFieldScope.() -> Unit = {}) =
   LabeledIntField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledFloatField(
   label: String, value: Float, onValueChange: (Float) -> Unit = {}, block: TextFieldScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      FloatField(value, onValueChange) { mod(); block() }
   }
}
fun UiScope.LabeledFloatField(label: String, value: MutableStateValue<Float>, block: TextFieldScope.() -> Unit = {}) =
   LabeledFloatField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledDoubleField(
   label: String, value: Double, onValueChange: (Double) -> Unit = {}, block: TextFieldScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      DoubleField(value, onValueChange) { mod(); block() }
   }
}
fun UiScope.LabeledDoubleField(label: String, value: MutableStateValue<Double>, block: TextFieldScope.() -> Unit = {}) =
   LabeledDoubleField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledVec2iField(
   label: String, value: Vec2i, onValueChange: (Vec2i) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      Vec2iField(value, onValueChange) { mod(); block() }
   }
}
fun UiScope.LabeledVec2iField(label: String, value: MutableStateValue<Vec2i>, block: UiScope.() -> Unit = {}) =
   LabeledVec2iField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledBooleanField(
   label: String, value: Boolean, onValueChange: (Boolean) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      Checkbox(value) {
         modifier.onToggle { onValueChange(it) }.padding(vertical = 4.dp, horizontal = 8.dp)
         block()
      }
      Text(if (value) "On" else "Off") {
         modifier.padding(4.dp).textAlignY(AlignmentY.Center)
            .onClick { onValueChange(!value) }
         mod()
      }
   }
}
fun UiScope.LabeledBooleanField(label: String, value: MutableStateValue<Boolean>, block: UiScope.() -> Unit = {}) =
   LabeledBooleanField(label, value.use(), { value.value = it }, block)

fun UiScope.LabeledColorField(label: String, value: Color, onValueChange: (Color) -> Unit = {}, block: UiScope.() -> Unit = {}) {
   LabeledField(label) {
      val hsv = value.toHsv()
      val hue = remember { mutableStateOf(hsv.h) }
      val sat = remember { mutableStateOf(hsv.s) }
      val `val` = remember { mutableStateOf(hsv.v) }
      val alpha = remember { mutableStateOf(value.a) }
      ColorChooser(hue, sat, `val`, alpha, onChange = onValueChange) { it(); block() }
   }
}
fun UiScope.LabeledColorField(label: String, value: MutableStateValue<Color>, block: UiScope.() -> Unit = {}) =
   LabeledColorField(label, value.use(), { value.value = it }, block)
