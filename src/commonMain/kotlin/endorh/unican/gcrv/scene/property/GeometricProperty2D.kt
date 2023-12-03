package endorh.unican.gcrv.scene.property

import de.fabmax.kool.math.Vec2f
import endorh.unican.gcrv.transformations.Transform2D

interface GeometricProperty2D {
   fun applyTransform(transform: Transform2D)
   val geometricCenter: Vec2f
   val geometricWeight: Float get() = 1F
}