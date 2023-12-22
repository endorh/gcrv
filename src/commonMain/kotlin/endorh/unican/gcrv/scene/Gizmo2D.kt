package endorh.unican.gcrv.scene

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.serialization.Color
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.squared
import endorh.unican.gcrv.util.toVec2f
import endorh.unican.gcrv.util.toVec2i

data class Rect2i(val left: Int, val top: Int, val right: Int, val bottom: Int) {
   val width get() = right - left
   val height get() = bottom - top

   val center get() = Vec2i((left + right) / 2, (top + bottom) / 2)
   val xRange get() = left..right
   val yRange get() = top..bottom
   val topLeft get() = Vec2i(left, top)
   val topRight get() = Vec2i(right, top)
   val bottomLeft get() = Vec2i(left, bottom)
   val bottomRight get() = Vec2i(right, bottom)

   operator fun contains(p: Vec2i) = p.x in xRange && p.y in yRange

   companion object {
      val ZERO = Rect2i(0, 0, 0, 0)
      val FULL = Rect2i(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
   }
}

interface Collider {
   val rectangle: Rect2i
   val center: Vec2i get() = rectangle.center
   fun fastContains(p: Vec2i): Boolean = p in rectangle && contains(p)
   operator fun contains(p: Vec2i): Boolean
   /**
    * Distance to the center of this collider, used to untie [Collider] clicks.
    */
   fun centerDistance(p: Vec2i): Float = rectangle.center.toVec2f().distance(p.toVec2f())

   object None : Collider {
      override val rectangle get() = Rect2i.ZERO
      override fun contains(p: Vec2i) = false
      override fun centerDistance(p: Vec2i) = Float.POSITIVE_INFINITY
   }
}

class TransformedCollider(val collider: Collider, val transform: Transform2D) : Collider {
   // TODO: I'm too lazy to implement this, could be useful if we ever want to display the rectangle
   //   To optimize collision it's faster to apply the inverse transform to the test point anyways
   override val rectangle: Rect2i get() = Rect2i.FULL
   override val center: Vec2i get() = transform.transform(collider.center)

   override fun fastContains(p: Vec2i) = collider.fastContains(transform.inverse.transform(p))
   override fun contains(p: Vec2i) = collider.contains(transform.inverse.transform(p))
}

class Rect2iCollider(override val rectangle: Rect2i) : Collider {
   override fun contains(p: Vec2i): Boolean = p in rectangle
}

class Line2iCollider(val line: Line2D, val radius: Int) : Collider {
   val c = line.c
   override val center = Vec2i((line.start.x + line.end.x) / 2, (line.start.y + line.end.y) / 2)
   val dx = line.dx
   val dy = line.dy
   val divSquare by lazy {
      line.lengthSquare
   }
   override val rectangle by lazy {
      Rect2i(
         minOf(line.start.x, line.end.x) - radius,
         minOf(line.start.y, line.end.y) - radius,
         maxOf(line.start.x, line.end.x) + radius,
         maxOf(line.start.y, line.end.y) + radius)
   }

   override fun contains(p: Vec2i): Boolean {
      val test = (dy * p.x - dx * p.y) - c
      val d = (p.x - center.x).squared + (p.y - center.y).squared
      return test*test < radius*radius * divSquare && 4*d < divSquare
   }

   override fun centerDistance(p: Vec2i) = center.toVec2f().distance(p.toVec2f())
}

class Point2iCollider(val point: Vec2i, val radius: Int) : Collider {
   override val rectangle by lazy {
      Rect2i(point.x - radius, point.y - radius, point.x + radius, point.y + radius)
   }
   override val center get() = point

   override fun contains(p: Vec2i): Boolean {
      val dx = p.x - point.x
      val dy = p.y - point.y
      return dx*dx + dy*dy < radius*radius
   }

   override fun centerDistance(p: Vec2i) = point.toVec2f().distance(p.toVec2f())
}

interface Gizmo2D {
   val collider: Collider

   /**
    * Occurs within a canvas update, which means implementations don't need to call [BufferCanvas.update].
    */
   fun render(canvas: BufferCanvas, transform: Transform2D)

   /**
    * The [position] is in geometric space (i.e., after applying any transforms inversely)
    */
   fun dragStart(position: Vec2f) {}
   /**
    * The [position] is in geometric space (i.e., after applying any transforms inversely)
    *
    * Implementations may also override [dragStart] and [dragEnd] to support stateful drag operations.
    */
   fun drag(position: Vec2f)
   /**
    * The [position] is in geometric space (i.e., after applying any transforms inversely)
    */
   fun dragEnd(position: Vec2f) {}
}

data class TransformedGizmo(val gizmo: Gizmo2D, val transform: Transform2D) {
   val collider: Collider get() = TransformedCollider(gizmo.collider, transform)
   fun render(canvas: BufferCanvas) = gizmo.render(canvas, transform)
   fun dragStart(pos: Vec2f) = gizmo.dragStart(transform.inverse.transform(pos))
   fun drag(pos: Vec2f) = gizmo.drag(transform.inverse.transform(pos))
   fun dragEnd(pos: Vec2f) = gizmo.dragEnd(transform.inverse.transform(pos))
}

class DrawGizmo(val renderer: PixelRendererContext.(Transform2D) -> Unit) : Gizmo2D {
   override val collider: Collider get() = Collider.None
   override fun render(canvas: BufferCanvas, transform: Transform2D) {
      CanvasPixelRendererContext(canvas).apply {
         renderer(transform)
      }
   }
   override fun drag(position: Vec2f) {}
}

class ControlPointGizmo(
   val value: () -> Vec2f, val style: ControlPointGizmoStyle = ControlPointGizmoStyle(),
   val onChange: GizmoDragListener
): Gizmo2D {
   override val collider get() = Point2iCollider(value().toVec2i(), 20)

   override fun render(canvas: BufferCanvas, transform: Transform2D) {
      with (CanvasPixelRendererContext(canvas)) {
         val pos = (transform * value()).toVec2i()
         renderPoint(Point2D(pos, style.outPoint))
         renderPoint(Point2D(pos, style.inPoint))
      }
   }

   override fun dragStart(position: Vec2f) {
      onChange.onDragStart(position)
   }
   override fun drag(position: Vec2f) {
      onChange.onDrag(position)
   }
   override fun dragEnd(position: Vec2f) {
      onChange.onDragEnd(position)
   }

   fun interface GizmoDragListener {
      fun onDragStart(pos: Vec2f) {}
      fun onDrag(pos: Vec2f)
      fun onDragEnd(pos: Vec2f) {}

      fun redirect(other: GizmoDragListener?) = other?.let {
         object : GizmoDragListener {
            override fun onDragStart(pos: Vec2f) {
               this@GizmoDragListener.onDragStart(pos)
               other.onDragStart(pos)
            }
            override fun onDrag(pos: Vec2f) {
               this@GizmoDragListener.onDrag(pos)
               other.onDrag(pos)
            }
            override fun onDragEnd(pos: Vec2f) {
               this@GizmoDragListener.onDragEnd(pos)
               other.onDragEnd(pos)
            }
         }
      } ?: this
   }

   data class ControlPointGizmoStyle(
      val outPoint: PointStyle = PointStyle(Color.LIGHT_GRAY, 15F, CircleAntiAliasPointRenderer),
      val inPoint: PointStyle = outPoint.copy(color=Color.DARK_GRAY, size=outPoint.size-4F)
   )
}