package endorh.unican.gcrv.ui2

import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <T> Delegates.observed(initialValue: T, crossinline onChange: (T) -> Unit) =
   observable(initialValue) { _, _, newValue -> onChange(newValue) }
inline fun <T> Delegates.lazySnapshot(noinline snapshotCounter: () -> Int, noinline computation: () -> T) =
   LazySnapshotProperty(snapshotCounter, computation)

class LazySnapshotProperty<T>(val snapshotCounter: () -> Int, val computation: () -> T) : ReadOnlyProperty<Any, T> {
   private var value: T? = null
   private var stateSnapshot = -1
   override fun getValue(thisRef: Any, property: KProperty<*>): T {
      val current = snapshotCounter()
      if (stateSnapshot != current) {
         value = computation()
         stateSnapshot = current
      }
      @Suppress("UNCHECKED_CAST")
      return value as T
   }
}
