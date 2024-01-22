package endorh.unican.gcrv.util

import de.fabmax.kool.modules.ui2.MutableState
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.UiSurface

open class NonPreemptiveMutableStateValue<T : Any?>(initValue: T) : MutableState() {
   private val stateListeners = mutableListOf<(T) -> Unit>()

   var value: T = initValue
      set(value) {
         val prev = field
         field = value
         if (prev != value) {
            stateChanged()
            notifyListeners(prev)
         }
      }

   private fun notifyListeners(prevValue: T) {
      if (stateListeners.isNotEmpty()) {
         for (i in stateListeners.indices) {
            stateListeners[i](prevValue)
         }
      }
   }

   fun set(value: T) {
      this.value = value
   }

   fun use(surface: UiSurface): T {
      usedBy(surface)
      return value
   }

   fun onChange(block: (T) -> Unit): NonPreemptiveMutableStateValue<T> {
      stateListeners += block
      return this
   }

   override fun toString(): String {
      return "nonPreemptiveMutableStateOf($value)"
   }
}

fun <T> nonPreemptiveMutableStateOf(value: T) = NonPreemptiveMutableStateValue(value)