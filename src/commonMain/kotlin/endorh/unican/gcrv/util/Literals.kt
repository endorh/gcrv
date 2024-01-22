package endorh.unican.gcrv.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import de.fabmax.kool.modules.ksl.lang.KslScalarExpression
import de.fabmax.kool.modules.ksl.lang.KslTypeInt1
import de.fabmax.kool.modules.ksl.lang.KslTypeUint1
import de.fabmax.kool.modules.ksl.lang.toFloat1
import de.fabmax.kool.util.Color
import kotlin.jvm.JvmInline

inline val Int.B get() = toByte()
inline val UInt.B get() = toUByte()
inline val Int.S get() = toShort()
inline val UInt.S get() = toUShort()

interface ColorFormat {
   val r: Int
   val g: Int
   val b: Int
   val a: Int
}

interface ColorFormatFloatAccessor {
   val value: ColorFormat
   val r: Float get() = value.r.toFloat() / 255F
   val g: Float get() = value.g.toFloat() / 255F
   val b: Float get() = value.b.toFloat() / 255F
   val a: Float get() = value.a.toFloat() / 255F
}

interface ColorFormatUnsignedAccessor {
   val value: ColorFormat
   val r: UInt get() = value.r.toUInt()
   val g: UInt get() = value.g.toUInt()
   val b: UInt get() = value.b.toUInt()
   val a: UInt get() = value.a.toUInt()
}

inline val Int.RGBA get() = RGBA(this)
inline val UInt.RGBA get() = RGBA(this.I)
@JvmInline value class RGBA(val value: Int) : ColorFormat {
   override val r: Int get() = value ushr 24
   override val g: Int get() = (value ushr 16) and 0xFF
   override val b: Int get() = (value ushr 8) and 0xFF
   override val a: Int get() = value and 0xFF

   inline val F: RGBAFloatAccessor get() = RGBAFloatAccessor(this)
   @JvmInline value class RGBAFloatAccessor(override val value: RGBA) : ColorFormatFloatAccessor
   inline val U: RGBAUnsignedAccessor get() = RGBAUnsignedAccessor(this)
   @JvmInline value class RGBAUnsignedAccessor(override val value: RGBA) : ColorFormatUnsignedAccessor {
      val I: UInt get() = value.value.toUInt()
   }
   inline val C: Color get() = Color(F.r, F.g, F.b, F.a)
   inline val I: Int get() = value
}

inline val Color.RGBA: RGBA get() = ((r * 255F).toUInt() and 0xFFu shl 24 or
  ((g * 255F).toUInt() and 0xFFu shl 16) or
  ((b * 255F).toUInt() and 0xFFu shl 8) or
  ((a * 255F).toUInt() and 0xFFu)).RGBA

inline val Int.U get() = toUInt()
inline val Int.L get() = toLong()
inline val Int.UL get() = toULong()
inline val Int.BI get() = toBigInteger()
inline val Long.U get() = toULong()
inline val Long.I get() = toInt()
inline val Long.UI get() = toUInt()
inline val Long.BI get() = toBigInteger()

inline val UInt.I get() = toInt()
inline val UInt.L get() = toLong()
inline val UInt.UL get() = toULong()
inline val UInt.BI get() = toBigInteger()
inline val ULong.I get() = toInt()
inline val ULong.UI get() = toUInt()
inline val ULong.L get() = toLong()
inline val ULong.BI get() = toBigInteger()

inline val Boolean.B get() = if (this) 1.B else 0.B
inline val Boolean.UB get() = if (this) 1U.B else 0U.B
inline val Boolean.S get() = if (this) 1.S else 0.S
inline val Boolean.US get() = if (this) 1U.S else 0U.S
inline val Boolean.I get() = if (this) 1 else 0
inline val Boolean.UI get() = if (this) 1U else 0U

inline val Int.F get() = toFloat()
inline val UInt.F get() = toFloat()
inline val Long.F get() = toFloat()
inline val ULong.F get() = toFloat()

inline val Int.D get() = toDouble()
inline val UInt.D get() = toDouble()
inline val Long.D get() = toDouble()
inline val ULong.D get() = toDouble()

inline val Int.BD get() = toBigDecimal()
inline val UInt.BD get() = if (I < 0) BigDecimal.fromBigInteger(toBigInteger()) else I.toBigDecimal()
inline val Long.BD get() = toBigDecimal()
inline val ULong.BD get() = if (L < 0) BigDecimal.fromBigInteger(toBigInteger()) else L.toBigDecimal()

inline val Float.I get() = toInt()
inline val Float.UI get() = toUInt()
inline val Float.L get() = toLong()
inline val Float.UL get() = toULong()
inline val Float.D get() = toDouble()
inline val Float.BI get() = BigInteger.tryFromFloat(this)
inline val Float.BD get() = toBigDecimal()

inline val Double.I get() = toInt()
inline val Double.UI get() = toUInt()
inline val Double.L get() = toLong()
inline val Double.UL get() = toULong()
inline val Double.F get() = toFloat()
inline val Double.BI get() = BigInteger.tryFromDouble(this)
inline val Double.BD get() = toBigDecimal()

inline val Float.NaNtoZero get() = if (isNaN()) 0F else this
inline val Double.NaNtoZero get() = if (isNaN()) 0F else this


inline val KslScalarExpression<KslTypeUint1>.F get() = toFloat1()
// inline val KslScalarExpression<KslTypeInt1>.F get() = toFloat1()