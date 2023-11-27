package endorh.unican.gcrv.util

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

operator fun Vec2i.component1() = x
operator fun Vec2i.component2() = y

operator fun Vec2f.component1() = x
operator fun Vec2f.component2() = y

interface Vec2fIntAccessor {
   val x: Int
   val y: Int

   operator fun component1() = x
   operator fun component2() = y
}

inline val Vec2f.floor get() = Vec2fFloorAccessor(this)
@JvmInline value class Vec2fFloorAccessor(val vec2f: Vec2f) : Vec2fIntAccessor {
   override val x get() = vec2f.x.toInt()
   override val y get() = vec2f.y.toInt()
}

inline val Vec2f.round get() = Vec2fRoundAccessor(this)
@JvmInline value class Vec2fRoundAccessor(val vec2f: Vec2f) : Vec2fIntAccessor {
   override val x get() = vec2f.x.roundToInt()
   override val y get() = vec2f.y.roundToInt()
}

fun Vec2f.toVec2i() = Vec2i(x.roundToInt(), y.roundToInt())
fun Vec2i.toVec2f() = Vec2f(x.F, y.F)
