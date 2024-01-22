package endorh.unican.gcrv.transformations

import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.serialization.Vec2fSerializer
import endorh.unican.gcrv.serialization.Vec2i
import endorh.unican.gcrv.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.*

@Serializable(Transform2D.Serializer::class)
class Transform2D private constructor(
   val a: Float, val b: Float, val c: Float, val d: Float,
   val e: Float, val f: Float
) {
   constructor(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float, inverse: Transform2D? = null) : this(a, b, c, d, e, f) {
      _inverse = inverse
   }
   fun transform(v: Vec2f) = Vec2f(v.x * a + v.y * b + e, v.x * c + v.y * d + f)
   fun transform(v: Vec2i) = Vec2i((v.x * a + v.y * b + e).I, (v.x * c + v.y * d + f).I)

   operator fun times(v: Vec2f) = transform(v)
   operator fun times(v: Vec2i) = transform(v)

   operator fun times(t: Transform2D) = Transform2D(
      a*t.a + b*t.c, a*t.b + b*t.d,
      c*t.a + d*t.c, c*t.b + d*t.d,
      a*t.e + b*t.f + e, c*t.e + d*t.f + f
   )

   infix fun linearTransform(v: Vec2f) = Vec2f(v.x * a + v.y * b, v.x * c + v.y * d)
   infix fun linearTransform(v: Vec2i) = Vec2i((v.x * a + v.y * b).I, (v.x * c + v.y * d).I)
   infix fun affineDisplacement(v: Vec2f) = Vec2f(v.x + e, v.y + f)
   infix fun affineDisplacement(v: Vec2i) = Vec2i((v.x + e).I, (v.y + f).I)

   @Transient private var _inverse: Transform2D? = null
   val inverse: Transform2D get() {
      _inverse?.let { return it }
      val det = (a*d - b*c)
      return if (det == 0F) identity else Transform2D(
         d / det, -b / det,
         -c / det, a / det,
         (b * f - d * e) / det, (c * e - a * f) / det,
         this).also { _inverse = it }
   }

   val linear: Transform2D by lazy {
      Transform2D(a, b, c, d, 0F, 0F)
   }

   fun localize(center: Vec2f) =
      translate(center) * this * translate(-center)

   override fun toString() = "[$a\t$b\t$e]\n[$c\t$d\t$f]\n[0\t0\t1]"

   companion object {
      val identity = Transform2D(1F, 0F, 0F, 1F, 0F, 0F)

      fun translate(x: Float, y: Float) = Transform2D(1F, 0F, 0F, 1F, x, y)
      fun translate(v: Vec2f) = Transform2D(1F, 0F, 0F, 1F, v.x, v.y)

      fun scale(s: Float) = Transform2D(s, 0F, 0F, s, 0F, 0F)
      fun scale(sx: Float, sy: Float) = Transform2D(sx, 0F, 0F, sy, 0F, 0F)
      fun scale(v: Vec2f) = scale(v.x, v.y)

      fun rotate(angle: Float) = Transform2D(cos(angle), -sin(angle), sin(angle), cos(angle), 0F, 0F)
      fun rotate(angle: Float, center: Vec2f) = translate(center) * rotate(angle) * translate(-center)

      fun reflectX() = Transform2D(1F, 0F, 0F, -1F, 0F, 0F)
      fun reflectY() = Transform2D(-1F, 0F, 0F, 1F, 0F, 0F)
      fun reflectY(center: Vec2f) = translate(center) * reflectY() * translate(-center)
      fun reflectX(center: Vec2f) = translate(center) * reflectX() * translate(-center)

      fun shearX(factor: Float) = Transform2D(1F, factor, 0F, 1F, 0F, 0F)
      fun shearY(factor: Float) = Transform2D(1F, 0F, factor, 1F, 0F, 0F)
      fun shear(factor: Vec2f) = Transform2D(1F, factor.x, factor.y, 1F, 0F, 0F)
   }

   object Serializer : KSerializer<Transform2D> {
      val listSerializer = ListSerializer(Vec2fSerializer)
      override val descriptor = listSerializer.descriptor

      override fun deserialize(decoder: Decoder): Transform2D {
         val list = listSerializer.deserialize(decoder)
         if (list.size != 3) throw SerializationException("Expected exactly 3 2D-vectors in transform!")
         val (m1, m2, t) = list
         return Transform2D(m1.x, m1.y, m2.x, m2.y, t.x, t.y)
      }
      override fun serialize(encoder: Encoder, value: Transform2D) = listSerializer.serialize(encoder, listOf(
         Vec2f(value.a, value.b),
         Vec2f(value.c, value.d),
         Vec2f(value.e, value.f)))
   }
}

@Serializable
data class TaggedTransform2D(
   val rotation: Float = 0F,
   val scale: Vec2f = Vec2f(1F, 1F),
   val translate: Vec2f = Vec2f.ZERO,
   val shearX: Float = 0F,
) {
   fun toTransform() = with (Transform2D) {
      translate(translate) * rotate(rotation) * scale(scale) * shearX(shearX)
   }

   companion object {
      val identity = TaggedTransform2D()

;      fun fromTransform(t: Transform2D): TaggedTransform2D {
         val translate = t * Vec2f.ZERO
         val linear = t.linear
         val tx = linear * Vec2f.X_AXIS
         val ty = linear * Vec2f.Y_AXIS
         val c = fromAxesPlusTranslation(tx, ty, translate)
         return c
      }

      fun fromThreePoints(bl: Vec2f, br: Vec2f, tl: Vec2f): TaggedTransform2D {
         val translate = (br + tl) / 2F
         val tx = (br - bl) / 2F
         val ty = (tl - bl) / 2F
         return fromAxesPlusTranslation(tx, ty, translate)
      }

      fun fromAxesPlusTranslation(tx: Vec2f, ty: Vec2f, offset: Vec2f): TaggedTransform2D {
         val shearX = tx.dot(ty) / tx.sqrLength()
         val tys = ty - tx * shearX
         val scale = Vec2f(tx.length(), tys.length() * tx.angleSign(tys))
         val rotation = atan2(tx.y, tx.x)
         return TaggedTransform2D(rotation, scale, offset, shearX)
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