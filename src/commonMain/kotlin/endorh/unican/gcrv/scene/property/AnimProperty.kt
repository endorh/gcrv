package endorh.unican.gcrv.scene.property

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.Easing
import endorh.unican.gcrv.animation.KeyFrame
import endorh.unican.gcrv.animation.KeyFrameList
import endorh.unican.gcrv.animation.TimeStamp
import endorh.unican.gcrv.ui2.LabeledField
import endorh.unican.gcrv.util.toTitleCase
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

abstract class AnimProperty<T>(private val defValue: T, val serializer: KSerializer<T>) : MutableState(), PropertyNode<AnimPropertyData<T>> {
   override var holder: PropertyHolder? = null
      get() = field.also {
         if (it == null) throw IllegalStateException("Animated property has no property holder")
      }
      set(value) {
         field = value
         value?.timeLine?.onChange {
            it?.currentTime?.onChange { notifyListeners(valueForTime(it)) }
         }
      }
   override lateinit var name: String
   override var priority: Int = 0

   override var isInternal: Boolean = false
      internal set
   val isDefault get() = timeLine.value == null && plainValue == defValue
   var isUnique = false
      private set
   var isAnimatable = true
      private set
   /**
    * Mark as unique.
    * Unique properties won't be editable when multiple objects are selected
    */
   fun unique() = apply { isUnique = true }
   fun static() = apply { isAnimatable = false }

   private val stateListeners = mutableListOf<(T) -> Unit>()
   val keyFrames = KeyFrameList<T>()
   val timeLine get() = holder!!.timeLine

   private var plainValue = defValue
   var value: T
      get() = timeLine.value?.let { timeLine ->
         valueForTime(timeLine.currentTime.value)
      } ?: plainValue
      set(value) {
         timeLine.value?.takeIf { keyFrames.isNotEmpty() } ?.let { timeLine ->
            val time = timeLine.currentTime.value
            val ref = keyFrames.ceilingKeyFrame(time) ?: keyFrames.floorKeyFrame(time)
            val interpolator = ref?.driver ?: defaultInterpolator
            val easing = ref?.easing ?: Easing.CubicBezier.Linear()
            keyFrames.set(KeyFrame(time, value, interpolator, easing))
         } ?: run { plainValue = value }
         notifyListeners(value)
      }

   open val availableDrivers: List<PropertyDriver<T>> = emptyList()

   fun valueForTime(time: TimeStamp) = keyFrames.valueForTime(time) ?: plainValue
   fun insertKeyFrame(time: TimeStamp) {
      val ref = keyFrames.ceilingKeyFrame(time) ?: keyFrames.floorKeyFrame(time)
      val interpolator = ref?.driver ?: defaultInterpolator
      val easing = ref?.easing?.copy() ?: Easing.CubicBezier.Linear()
      keyFrames.set(KeyFrame(time, valueForTime(time), interpolator, easing))
      stateChanged()
   }
   fun removeKeyFrame(time: TimeStamp) {
      if (keyFrames.size == 1) keyFrames[time]?.let {
         plainValue = it.value
      }
      keyFrames.remove(time)
      stateChanged()
   }

   val defaultInterpolator get() = availableDrivers.firstOrNull() ?: PropertyDriver.None()

   private fun notifyListeners(newState: T) {
      stateChanged()
      for (listener in stateListeners) listener(newState)
   }

   fun use(surface: UiSurface): T = value.also {
      usedBy(surface)
   }
   override fun onChange(block: () -> Unit) {
      onChange { _ -> block() }
   }
   @JvmName("onChangeT")
   fun onChange(block: (T) -> Unit) = apply {
      stateListeners += block
   }
   override fun clearChangeListeners() {
      stateListeners.clear()
   }

   // operator fun UiScope.getValue(thisRef: Any?, property: KProperty<*>) = use(surface)
   operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
   operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
      this.value = value
   }

   override fun toString() = holder?.let { "AnimProperty($value)" } ?: "AnimProperty(<uninitialized>)"

   override fun save() =
      if (keyFrames.isNotEmpty()) AnimPropertyData(null, keyFrames)
      else AnimPropertyData(plainValue, null)
   @OptIn(ExperimentalSerializationApi::class)
   override fun load(data: AnimPropertyData<T>) {
      if (data.keyFrames != null) {
         keyFrames.clear()
         keyFrames.setAll(data.keyFrames.allKeyFrames.asIterable())
      } else keyFrames.clear()
      if (data.value != null || serializer.descriptor.isNullable) {
         @Suppress("UNCHECKED_CAST")
         (this as AnimProperty<T?>).plainValue = data.value
      }
   }
   override val saveSerializer get() =
      AnimPropertyData.ConciseJsonSerializer(serializer, availableDrivers)
   open val animatedEditorTint = Color("FF42FF80")
   open val editorTint: Color? get() = if (keyFrames.isNotEmpty()) animatedEditorTint else null
   abstract fun UiEditorScope<T>.editor(value: T, modifier: UiScope.() -> Unit = {})
   open fun UiScope.editor(
      selected: Collection<AnimProperty<T>> = emptyList(),
      onFocus: () -> Unit = {},
      modifier: UiModifier.() -> UiModifier = { this }
   ) {
      use(surface)
      LabeledField(name.toTitleCase(), modifier) {
         this.modifier.onClick {
            onFocus()
         }
         UiEditorScope.Impl(this@AnimProperty, selected, this).editor(value, it)
      }
   }

   /*open class Serializer<T, P: AnimProperty<T>>(private val factory: (T) -> P, private val serializer: KSerializer<T>) : KSerializer<P> {
      override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AnimProperty", serializer.descriptor) {
         element("value", serializer.descriptor)
         element("keyFrames", KeyFrameList.serializer(serializer).descriptor, isOptional = true)
      }
      override fun serialize(encoder: Encoder, value: P) {
         encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer, value.plainValue)
            if (value.keyFrames.isNotEmpty()) {
               encodeSerializableElement(descriptor, 1, KeyFrameList.serializer(serializer), value.keyFrames)
            }
         }
      }
      override fun deserialize(decoder: Decoder): P = decoder.decodeStructure(descriptor) {
         var valueWritten = false
         var value: T? = null
         var keyFrames: KeyFrameList<T>? = null
         while (true) {
            when (val index = decodeElementIndex(descriptor)) {
               CompositeDecoder.DECODE_DONE -> break
               0 -> value = decodeSerializableElement(descriptor, 0, serializer, value).also { valueWritten = true }
               1 -> keyFrames = decodeSerializableElement(descriptor, 1, KeyFrameList.serializer(serializer), keyFrames)
               else -> throw SerializationException("Unexpected serial index: $index")
            }
         }
         if (!valueWritten) throw SerializationException("Missing value for AnimProperty!")
         @Suppress("UNCHECKED_CAST")
         value as T
         val p = factory(value)
         keyFrames?.let {
            p.keyFrames.setAll(it.allKeyFrames.asIterable())
         }
         p
      }
   }*/
}