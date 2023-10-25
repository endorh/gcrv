package endorh.unican.gcrv.util

import de.fabmax.kool.util.Color
import kotlin.jvm.JvmInline

interface IntAccessor1D {
   operator fun get(index: Int): Int
   operator fun set(index: Int, value: Int)

   operator fun get(index: UInt) = get(index.I)
   operator fun set(index: UInt, value: Int) = set(index.I, value)
}

interface IntAccessor2D {
   operator fun get(x: Int, y: Int): Int
   operator fun set(x: Int, y: Int, value: Int)

   operator fun get(x: UInt, y: UInt) = get(x.I, y.I)
   operator fun set(x: UInt, y: UInt, value: Int) = set(x.I, y.I, value)
}

interface UIntAccessor1D {
   operator fun get(index: Int): UInt
   operator fun set(index: Int, value: UInt)

   operator fun get(index: UInt) = get(index.I)
   operator fun set(index: UInt, value: UInt) = set(index.I, value)
}

@JvmInline value class IntUIntAccessor1D(val accessor: IntAccessor1D) {
   operator fun get(index: Int): UInt = accessor[index].toUInt()
   operator fun set(index: Int, value: UInt) = accessor.set(index, value.I)
}

interface UIntAccessor2D {
   operator fun get(x: Int, y: Int): UInt
   operator fun set(x: Int, y: Int, value: UInt)

   operator fun get(x: UInt, y: UInt) = get(x.I, y.I)
   operator fun set(x: UInt, y: UInt, value: UInt) = set(x.I, y.I, value)
}

interface ColorAccessor1D {
   operator fun get(index: Int): Color
   operator fun set(index: Int, value: Color)

   operator fun get(index: UInt) = get(index.I)
   operator fun set(index: UInt, value: Color) = set(index.I, value)
}

interface ColorAccessor2D {
   operator fun get(x: Int, y: Int): Color
   operator fun set(x: Int, y: Int, value: Color)

   operator fun get(x: UInt, y: UInt) = get(x.I, y.I)
   operator fun set(x: UInt, y: UInt, value: Color) = set(x.I, y.I, value)
}

interface FencedAccessor1D {
   val fenceStart: Int get() = 0
   val fenceLimit: Int

   operator fun get(index: Int): Int
   operator fun set(index: Int, value: Int)

   operator fun get(index: UInt) = get(index.I)
   operator fun set(index: UInt, value: Int) = set(index.I, value)
}

interface FencedAccessor2D {
   val fenceWidth: Int
   val fenceHeight: Int

   operator fun get(x: Int, y: Int): Int
   operator fun set(x: Int, y: Int, value: Int)

   operator fun get(x: UInt, y: UInt) = get(x.I, y.I)
   operator fun set(x: UInt, y: UInt, value: Int) = set(x.I, y.I, value)
}