@file:OptIn(ExperimentalUnsignedTypes::class)

package endorh.unican.gcrv.util

import de.fabmax.kool.util.Uint8Buffer

// expect fun Uint8Buffer.asUint32Buffer(): Uint32Buffer

fun Uint8Buffer.getInt(i: Int): Int {
   val b = i * 4
   return this[b].toInt() shl 24 or (this[b + 1].toInt() shl 16) or
     (this[b + 2].toInt() shl 8) or this[b + 3].toInt()
}

fun Uint8Buffer.setInt(i: Int, v: Int) {
   val b = i * 4
   this[b] = (v ushr 24).toByte()
   this[b + 1] = (v ushr 16).toByte()
   this[b + 2] = (v ushr 8).toByte()
   this[b + 3] = v.toByte()
}

fun Uint8Buffer.getUInt(i: Int): UInt {
   val b = i * 4
   return this[b].toUInt() shl 24 or (this[b + 1].toUInt() shl 16) or
     (this[b + 2].toUInt() shl 8) or this[b + 3].toUInt()
}

fun Uint8Buffer.setUInt(i: Int, v: UInt) {
   val b = i * 4
   this[b] = (v shr 24).toByte()
   this[b + 1] = (v shr 16).toByte()
   this[b + 2] = (v shr 8).toByte()
   this[b + 3] = v.toByte()
}

fun IntArray.toByteArray(): ByteArray {
   val dest = ByteArray(size * 4)
   for (i in indices) {
      dest[i * 4] = (this[i] ushr 24).toByte()
      dest[i * 4 + 1] = (this[i] ushr 16).toByte()
      dest[i * 4 + 2] = (this[i] ushr 8).toByte()
      dest[i * 4 + 3] = this[i].toByte()
   }
   return dest
}

fun ByteArray.toIntArray(): IntArray {
   val dest = IntArray((size + 3) / 4)
   for (i in dest.indices) {
      dest[i] = (this[i * 4].toInt() shl 24) or (this[i * 4 + 1].toInt() shl 16) or
        (this[i * 4 + 2].toInt() shl 8) or this[i * 4 + 3].toInt()
   }
   return dest
}

fun Uint8Buffer.put(array: IntArray) {
   put(array.toByteArray())
}
fun Uint8Buffer.put(array: IntArray, offset: Int, length: Int) {
   put(array.toByteArray(), offset * 4, length * 4)
}

fun Uint8Buffer.put(array: UIntArray) {
   put(array.toIntArray().toByteArray())
}
fun Uint8Buffer.put(array: UIntArray, offset: Int, length: Int) {
   put(array.toIntArray(), offset, length)
}