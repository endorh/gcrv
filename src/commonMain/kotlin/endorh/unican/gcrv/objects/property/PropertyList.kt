package endorh.unican.gcrv.objects.property

import kotlin.reflect.KProperty

class PropertyList<T: PropertyNode>(
   val factory: () -> T, values: List<T> = emptyList(), val showExpanded: Boolean = true
) : PropertyNode {
   override var holder: PropertyHolder? = null
      set(value) {
         field = value
         for (p in entries) p.init(p.name, value)
      }
   override lateinit var name: String
   override var priority: Int = 0
   private val mutableEntries = mutableListOf<T>()
   val entries: List<T> get() = mutableEntries
   init {
      mutableEntries.addAll(values)
   }

   private val changeListeners = mutableListOf<() -> Unit>()
   override fun onChange(block: () -> Unit) {
      changeListeners += block
   }
   override fun clearChangeListeners() {
      changeListeners.clear()
   }

   operator fun get(i: Int) = entries[i]

   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   fun insert() {
      mutableEntries.add(factory().apply {
         init(size.toString(), this@PropertyList.holder)
         onChange { stateChanged() }
      })
      stateChanged()
   }
   fun remove() {
      mutableEntries.removeLast()
      stateChanged()
   }

   fun stateChanged() {
      for (listener in changeListeners) listener()
   }

   val size get() = entries.size
}