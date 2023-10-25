package endorh.unican.gcrv.util

import kotlin.jvm.JvmInline

/**
 * Row-major 2D array of bytes.
 */
class ByteArray2D(val width: Int, val height: Int) {
   private val byteArray = ByteArray(width * height)

   operator fun get(x: Int, y: Int) = byteArray[y * width + x]
   operator fun set(x: Int, y: Int, value: Byte) {
      byteArray[y * width + x] = value
   }

   /**
    * Column-major access
    */
   inline val CM get() = ColumnMajorAccessor(this)
   @JvmInline value class ColumnMajorAccessor(val array: ByteArray2D) {
      operator fun get(x: Int, y: Int) = array.byteArray[x * array.height + y]
      operator fun set(x: Int, y: Int, value: Byte) {
         array.byteArray[x * array.height + y] = value
      }
   }

   fun asByteArray() = byteArray
   fun loadByteArray(array: ByteArray, offset: Int = 0, startIndex: Int = 0, endIndex: Int = array.size) {
      array.copyInto(byteArray, offset, startIndex, endIndex)
   }
}

/**
 * Row-major 2D array of ints.
 */
class IntArray2D(val width: Int, val height: Int) {
   private val intArray = IntArray(width * height)

   operator fun get(x: Int, y: Int) = intArray[y * width + x]
   operator fun set(x: Int, y: Int, value: Int) {
      intArray[y * width + x] = value
   }

   /**
    * Column-major access
    */
   inline val CM get() = ColumnMajorAccessor(this)
   @JvmInline value class ColumnMajorAccessor(val array: IntArray2D) {
      operator fun get(x: Int, y: Int) = array.intArray[x * array.height + y]
      operator fun set(x: Int, y: Int, value: Int) {
         array.intArray[x * array.height + y] = value
      }
   }

   fun asIntArray() = intArray
   fun loadIntArray(array: IntArray, offset: Int = 0, startIndex: Int = 0, endIndex: Int = array.size) {
      array.copyInto(intArray, offset, startIndex, endIndex)
   }
}