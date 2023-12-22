package endorh.unican.gcrv.util

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.math.Vec4i

fun ULong.toVec2i() = Vec2i((this shr 32).I, (this and 0xFFFF_FFFF_UL).I)
fun ULong.toVec4i() = Vec4i(0, 0, (this shr 32).I, (this and 0xFFFF_FFFF_UL).I)
fun Long.toVec2i() = Vec2i((this shr 32).I, (this and 0xFFFF_FFFFL).I)
fun Long.toVec4i() = Vec4i(
   if (this < 0L) -1 else 0, if (this < 0L) -1 else 0, (this shr 32).I, (this and 0xFFFF_FFFFL).I)