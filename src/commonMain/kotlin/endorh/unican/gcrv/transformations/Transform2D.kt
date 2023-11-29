package endorh.unican.gcrv.transformations

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import endorh.unican.gcrv.util.*
import kotlin.math.*

class Transform2D(
   val a: Float, val b: Float, val c: Float, val d: Float,
   val e: Float, val f: Float
) {
   fun transform(v: Vec2f) = Vec2f(v.x * a + v.y * b + e, v.x * c + v.y * d + f)
   fun transform(v: Vec2i) = Vec2i((v.x * a + v.y * b + e).I, (v.x * c + v.y * d + f).I)

   operator fun times(v: Vec2f) = transform(v)
   operator fun times(v: Vec2i) = transform(v)

   operator fun times(t: Transform2D) = Transform2D(
      a*t.a + b*t.c, a*t.b + b*t.d,
      c*t.a + d*t.c, c*t.b + d*t.d,
      a*t.e + b*t.f + e, c*t.e + d*t.f + f
   )

   fun inverse() = (a*d - b*c).let { det ->
      if (det == 0F) identity else Transform2D(
         d/det, -b/det,
         -c/det, a/det,
         (b*f - d*e)/det, (c*e - a*f)/det)
   }

   fun localize(center: Vec2f) =
      translate(center) * this * translate(-center)

   override fun toString() = "[$a\t$b\t$e]\n[$c\t$d\t$f]\n[0\t0\t1]"

   companion object {
      val identity = Transform2D(1F, 0F, 0F, 1F, 0F, 0F)

      fun translate(v: Vec2f) = Transform2D(1F, 0F, 0F, 1F, v.x, v.y)

      fun scale(s: Float) = Transform2D(s, 0F, 0F, s, 0F, 0F)
      fun scale(sx: Float, sy: Float) = Transform2D(sx, 0F, 0F, sy, 0F, 0F)
      fun scale(v: Vec2f) = scale(v.x, v.y)

      fun rotate(angle: Float) = Transform2D(cos(angle), -sin(angle), sin(angle), cos(angle), 0F, 0F)
      fun rotate(angle: Float, center: Vec2f) = translate(center) * rotate(angle) * translate(-center)

      fun reflectX() = Transform2D(1F, 0F, 0F, -1F, 0F, 0F)
      fun reflectY() = Transform2D(-1F, 0F, 0F, 1F, 0F, 0F)
      fun reflectY(center: Vec2f) = translate(center) * reflectY() * translate(-center)

      fun shearX(factor: Float) = Transform2D(1F, factor, 0F, 1F, 0F, 0F)
      fun shearY(factor: Float) = Transform2D(1F, 0F, factor, 1F, 0F, 0F)
      fun shear(factor: Vec2f) = Transform2D(1F, factor.x, factor.y, 1F, 0F, 0F)
   }
}

class TaggedTransform2D(
   val rotation: Float,
   val scale: Vec2f,
   val translate: Vec2f,
   val shearY: Float,
) {
   fun toTransform() = with (Transform2D) {
      translate(translate) * rotate(rotation) * shearY(shearY) * scale(scale)
   }

   companion object {
      private val PI_F = PI.F

;      fun fromTransform(t: Transform2D): TaggedTransform2D {
         val translate = t * Vec2f.ZERO
         val linear = Transform2D.translate(-translate) * t
         val tx = linear * Vec2f.X_AXIS
         val ty = linear * Vec2f.Y_AXIS
         val rotation = Vec2f.X_AXIS.signedAngle(tx)
         val axisAngle = tx.signedAngle(ty)
         val shearAngle = if (axisAngle < 0) axisAngle + PI_F else axisAngle
         val shearY = ctg(shearAngle).roundDecimals(4)
         val tys = ty - tx * (tx * ty)
         val scale = Vec2f(tx.length(), tys.length() * sign(axisAngle))
         val r = TaggedTransform2D(rotation, scale, translate, shearY)
         println("Conversion: $t -> ${r.toTransform()}")
         return r
      }
   }
}

class Transform2DStack {
   private val mutableStack = mutableListOf<Transform2D>()
   val stack: List<Transform2D> = mutableStack

   private val aggregated = mutableListOf<Transform2D>()
   var transform = Transform2D.identity
      private set

   fun push(t: Transform2D) {
      mutableStack += t
      val p = transform * t
      aggregated += p
      transform = p
   }

   fun pop() {
      mutableStack.removeLast()
      aggregated.removeLast()
      transform = aggregated.lastOrNull() ?: Transform2D.identity
   }

   fun with(t: Transform2D, block: (Transform2D) -> Unit) {
      push(t)
      block(transform)
      pop()
   }
}