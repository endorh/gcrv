package endorh.unican.gcrv.util

import de.fabmax.kool.modules.ksl.lang.*

fun KslScopeBuilder.uint4Const(v: UInt) = uint4Const(v, v, v, v)
fun KslScopeBuilder.uint4Const(x: UInt, y: UInt, z: UInt, w: UInt) = inlineCode("uint4($x, $y, $z, $w)")

fun KslScopeBuilder.uint1VarAssignInt1(vr: KslVarScalar<KslTypeUint1>, v: KslExpression<KslTypeInt1>) =
   inlineCode("${vr.toPseudoCode()} = uint(${v.toPseudoCode()});")
fun KslScopeBuilder.uint4VarAssignInt4(vr: KslVarVector<KslTypeUint4, KslTypeUint1>, v: KslExpression<KslTypeInt4>) =
   inlineCode("${vr.toPseudoCode()} = uvec4(${v.toPseudoCode()});")

// @Suppress("UNCHECKED_CAST")
fun KslScopeBuilder.uInt1Array(arraySize: Int, initExpr: KslExprUint1, name: String? = null): KslArrayScalar<KslTypeUint1> {
   return KslArrayScalar(name ?: nextName("u1Array"), KslTypeUint1, arraySize, true).also { definedStates += it }.also {
      ops += KslDeclareArray(it, initExpr, this)
   }
   // return uint1Array(arraySize, initExpr, name) as KslArrayScalar<KslTypeUint1>
}