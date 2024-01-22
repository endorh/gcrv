package endorh.unican.gcrv.scene.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.mutableStateListOf
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.Object2DType

class GroupObject2D : Object2D(Type) {
   override val children = mutableStateListOf<Object2D>()

   override val geometricCenter: Vec2f
      get() {
      var x = 0F
      var y = 0F
      val s = children.size
      for (child in children) {
         val center = child.geometricCenter
         x += center.x
         y += center.y
      }
      return Vec2f(x / s, y / s)
   }

   override fun toString() = "Group[${children.joinToString()}]"

   companion object Type : Object2DType<GroupObject2D>("group") {
      private var groupCount = 0
      override fun generateName() = "Group ${++groupCount}"
      override fun create() = GroupObject2D()
   }
}