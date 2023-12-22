package endorh.unican.gcrv.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger

@OptIn(ExperimentalUnsignedTypes::class)
fun BigDecimal.toMultiIntArray(arraySize: Int, intSize: Int, div: Int): UIntArray {
   val array = UIntArray(arraySize) { 0u }
   val int = (this * BigDecimal.fromIntWithExponent(2, div * 32L)).toBigInteger()
   for (i in 0 until intSize) {
      val p = BigInteger.fromInt(1) shl (i + 1)*32
      array[i] = (int % p shr i*32).uintValue(false)
   }
   return array
}