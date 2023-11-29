package endorh.unican.gcrv.objects.property

import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import endorh.unican.gcrv.animation.TimeLine
import kotlin.reflect.KProperty

abstract class CompoundAnimProperty : PropertyHolder, PropertyNode {
   override var holder: PropertyHolder? = null
      set(value) {
         field = value
         for (p in properties.values) {
            p.init(p.name, this)
         }
      }
   override lateinit var name: String
   override var priority: Int = 0
   override val properties: PropertyMap = PropertyMap()
   override val timeLine: MutableStateValue<TimeLine?> get() = holder?.timeLine ?: mutableStateOf(null)
   open val showExpanded get() = true

   private val changeListeners = mutableListOf<() -> Unit>()
   override fun onChange(block: () -> Unit) {
      changeListeners += block
   }
   override fun clearChangeListeners() {
      changeListeners.clear()
   }

   protected operator fun <N: PropertyNode> N.provideDelegate(thisRef: CompoundAnimProperty, property: KProperty<*>): N = apply {
      init(property.name, thisRef)
      thisRef.register(this)
      onChange { thisRef.stateChanged() }
   }

   fun stateChanged() {
      for (listener in changeListeners) listener()
   }
}