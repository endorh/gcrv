package endorh.unican.gcrv.line_algorithms.ui

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.*
import endorh.unican.gcrv.line_algorithms.renderers.OptionIdxPicker
import endorh.unican.gcrv.ui2.ColorField
import endorh.unican.gcrv.util.removeTrailingZeros
import endorh.unican.gcrv.util.roundToString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun String.onlyNumberChars() = filter { it.isDigit() || it in "+- " }
fun String.onlyDecimalNumberChars() = filter { it.isDigit() || it in "+-.eE" }

class HoverState : MutableStateValue<Boolean>(false), Hoverable {
   override fun onEnter(ev: PointerEvent) {
      value = true
   }
   override fun onExit(ev: PointerEvent) {
      value = false
   }
}

fun Color.mix(tint: Color?, amount: Float) = if (tint != null) mix(tint, amount) else this

@OptIn(ExperimentalContracts::class)
inline fun UiScope.BlendTextField(
   text: String = "",
   scopeName: String? = null,
   tint: Color? = null,
   padding: Dp = 4.dp,
   suppressBackground: Boolean = false,
   block: TextFieldScope.() -> Unit
): TextFieldScope {
   contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
   }

   val textField = uiNode.createChild(scopeName, TextFieldNode::class, TextFieldNode.factory)
   if (textField.isFocused.use()) {
      surface.onEachFrame(textField::updateCaretBlinkState)
   }
   val hoverState = remember { HoverState() }
   return textField.apply {
      modifier
         .text(text)
         .onClick(textField)
         .hoverListener(textField)
         .hoverListener(hoverState)
         .dragListener(textField)
      if (!suppressBackground || isFocused.use() || hoverState.use()) modifier.background(
         RoundRectBackground(
            (if (isFocused.value) colors.backgroundAlpha(0.6F)
            else if (hoverState.value) colors.backgroundVariant.mix(Color.WHITE, 0.2F)
            else colors.backgroundVariant).mix(tint, 0.4F), padding))
      modifier.padding(padding).apply {
         lineColor = null
         lineFocusedColor = null
      }
      block()
   }
}

fun UiScope.StringField(
   value: String, onValueChange: (String) -> Unit = {},
   scopeName: String? = null, tint: Color? = null, padding: Dp = 4.dp, suppressBackground: Boolean = false,
   block: TextFieldScope.() -> Unit = {}
) {
   BlendTextField(value, scopeName, tint, padding, suppressBackground) {
      modifier.onChange(onValueChange)
      block()
   }
}

fun UiScope.IntField(
   value: Int, onValueChange: (Int) -> Unit = {}, scopeName: String? = null,
   tint: Color? = null, padding: Dp = 4.dp, suppressBackground: Boolean = false,
   block: TextFieldScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   BlendTextField(invalidStringState.value ?: value.toString(), scopeName, tint, padding, suppressBackground) {
      modifier.textAlignX(AlignmentX.End).onChange { s ->
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
   tint: Color? = null, padding: Dp = 4.dp, suppressBackground: Boolean = false,
   decimals: Int = 3,
   block: TextFieldScope.() -> Unit = {}
) {
   val stringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      stringState.value = null
   }
   BlendTextField(stringState.value ?: value.roundToString(decimals).removeTrailingZeros(), scopeName, tint, padding, suppressBackground) {
      modifier.textAlignX(AlignmentX.End).onChange { s ->
         val sanitized = s.onlyDecimalNumberChars()
         stringState.value = sanitized
         sanitized.toFloatOrNull()?.let {
            lastValue.value = it
            onValueChange(it)
         }
      }
      block()
   }
}

fun UiScope.DoubleField(
   value: Double, onValueChange: (Double) -> Unit = {}, scopeName: String? = null,
   tint: Color? = null, padding: Dp = 4.dp, suppressBackground: Boolean = false,
   block: TextFieldScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   BlendTextField(invalidStringState.value ?: value.toString(), scopeName, tint, padding, suppressBackground) {
      modifier.background(RoundRectBackground(colors.backgroundVariant, 2.dp))
      modifier.textAlignX(AlignmentX.End).onChange { s ->
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
   tint: Color? = null, padding: Dp = 4.dp,
   rowModifier: (UiModifier) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   Row {
      this.modifier.background(RoundRectBackground(colors.backgroundVariant.mix(tint, 0.4F), 4.dp))
      rowModifier(modifier)
      IntField(value.x, { onValueChange(Vec2i(it, value.y)) }, scopeName?.let { "$it/x" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(end = 4.dp)
            .padding(end=0.dp)
      }
      IntField(value.y, { onValueChange(Vec2i(value.x, it)) }, scopeName?.let { "$it/y" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(start = 4.dp)
            .padding(start=1.dp)
      }
      block()
   }
}

fun UiScope.Vec2fField(
   value: Vec2f, onValueChange: (Vec2f) -> Unit = {}, scopeName: String? = null,
   tint: Color? = null, padding: Dp = 4.dp,
   rowModifier: (UiModifier) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   Row {
      this.modifier.background(RoundRectBackground(colors.backgroundVariant.mix(tint, 0.4F), 4.dp))
      rowModifier(modifier)
      FloatField(value.x, { onValueChange(Vec2f(it, value.y)) }, scopeName?.let { "$it/x" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(end = 4.dp)
            .padding(end=0.dp)
      }
      FloatField(value.y, { onValueChange(Vec2f(value.x, it)) }, scopeName?.let { "$it/y" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(start = 4.dp)
            .padding(start=1.dp)
      }
      block()
   }
}

fun UiScope.TimeStampField(
   value: TimeStamp, onValueChange: (TimeStamp) -> Unit = {}, scopeName: String? = null,
   tint: Color? = null, padding: Dp = 4.dp, suppressBackground: Boolean = false,
   block: UiScope.() -> Unit = {}
) {
   val invalidStringState = remember { mutableStateOf<String?>(null) }
   val lastValue = remember { mutableStateOf(value) }
   if (value != lastValue.value) {
      lastValue.value = value
      invalidStringState.value = null
   }
   BlendTextField(
      invalidStringState.value ?: value.asSeconds().roundToString(3),
      scopeName, tint, padding, suppressBackground
   ) {
      modifier.textAlignX(AlignmentX.End).onChange { s ->
         val sanitized = s.onlyDecimalNumberChars()
         sanitized.toFloatOrNull()?.let {
            onValueChange(TimeStamp.fromSeconds(it))
            invalidStringState.value = null
         } ?: run { invalidStringState.value = sanitized }
      }
      block()
   }
}

fun UiScope.TimeRangeField(
   value: TimeRange, onValueChange: (TimeRange) -> Unit = {}, scopeName: String? = null,
   tint: Color? = null, padding: Dp = 4.dp,
   rowModifier: (UiModifier) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   Row {
      this.modifier.background(RoundRectBackground(colors.backgroundVariant.mix(tint, 0.4F), 4.dp))
      rowModifier(modifier)
      TimeStampField(value.start, { onValueChange(TimeRange(it, value.end, value.exclusiveEnd)) }, scopeName?.let { "$it/x" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(end = 4.dp)
            .padding(end = 0.dp)
      }
      TimeStampField(value.end, { onValueChange(TimeRange(value.start, it, value.exclusiveEnd)) }, scopeName?.let { "$it/y" }, null, padding, true) {
         modifier.width(Grow(0.5F, min = 40.dp)).margin(start = 4.dp)
            .padding(start = 1.dp)
      }
      block()
   }
}

internal fun UiScope.LabeledField(
   label: String, modifier: UiModifier.() -> UiModifier = { this },
   field: UiScope.(UiScope.() -> Unit) -> Unit
) {
   Row(scopeName = "Field/$label") {
      this.modifier.margin(horizontal=4.dp).padding(2.dp).width(Grow.Std)
      this.modifier.modifier()
      Text(label) {
         this.modifier
            .width(Grow(0.3F))
            .padding(end = 8.dp)
            .alignY(AlignmentY.Center)
      }
      field {
         this.modifier.width(Grow(0.7F))
      }
   }
}

fun UiScope.LabeledStringField(
   label: String, value: String, onValueChange: (String) -> Unit = {}, block: TextFieldScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      BlendTextField(value) { modifier.onChange { onValueChange(it) }; mod(); block() }
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

fun UiScope.LabeledVec2fField(
   label: String, value: Vec2f, onValueChange: (Vec2f) -> Unit = {}, block: UiScope.() -> Unit = {}
) {
   LabeledField(label) { mod ->
      Vec2fField(value, onValueChange) { mod(); block() }
   }
}
fun UiScope.LabeledVec2fField(label: String, value: MutableStateValue<Vec2f>, block: UiScope.() -> Unit = {}) =
   LabeledVec2fField(label, value.use(), { value.value = it }, block)

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
      ColorField(value, onValueChange) { it(); block() }
   }
}
fun UiScope.LabeledColorField(label: String, value: MutableStateValue<Color>, block: UiScope.() -> Unit = {}) =
   LabeledColorField(label, value.use(), { value.value = it }, block)
