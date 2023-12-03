package endorh.unican.gcrv.serialization

import kotlinx.serialization.KSerializer

interface Savable<T> {
   fun save(): T
   fun load(data: T)
}

interface SavableSerializer<T> : Savable<T> {
   val saveSerializer: KSerializer<T>
}