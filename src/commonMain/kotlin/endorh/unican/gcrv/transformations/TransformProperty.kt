package endorh.unican.gcrv.transformations

import de.fabmax.kool.math.Vec2f
import endorh.unican.gcrv.objects.CompoundAnimProperty
import endorh.unican.gcrv.objects.float
import endorh.unican.gcrv.objects.vec2f
import endorh.unican.gcrv.util.rad
import kotlin.reflect.KProperty

class TransformProperty : CompoundAnimProperty() {
   var translate by vec2f(Vec2f.ZERO)
   var rotation by float(0F)
   var scale by vec2f(Vec2f(1F, 1F))
   var shearX by float(0F)

   operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

   fun setFromTaggedTransform(t: TaggedTransform2D) {
      translate = t.translate
      rotation = t.rotation
      scale = t.scale
      shearX = t.shearX
   }

   fun setFrom(t: Transform2D) = setFromTaggedTransform(TaggedTransform2D.fromTransform(t))

   val transform get() = with(Transform2D) {
      translate(translate) * rotate(rad(rotation)) * shearX(shearX) * scale(scale)
   }

   val taggedTransform get() = TaggedTransform2D(rotation, scale, translate, shearX)
}