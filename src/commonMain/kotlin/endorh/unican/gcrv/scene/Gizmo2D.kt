package endorh.unican.gcrv.scene

import de.fabmax.kool.demo.Settings
import de.fabmax.kool.math.Vec2f
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.serialization.Color
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.*

/**
 * A handle used to interact with 2D objects in a canvas.
 *
 * Gizmos can respond to being dragged in geometric space.
 */
interface Gizmo2D {
   val collider: Collider2D

   /**
    * Occurs within a canvas update, which means implementations don't need to call [BufferCanvas.update].
    */
   fun render(canvas: BufferCanvas, transform: Transform2D)

   /**
    * The [position] is in geometric space (i.e., after inverting the canvas transforms)
    */
   fun dragStart(position: Vec2f) {}
   /**
    * The [position] is in geometric space (i.e., after inverting the canvas transforms)
    *
    * Implementations may also override [dragStart] and [dragEnd] to support stateful drag operations.
    */
   fun drag(position: Vec2f)
   /**
    * The [position] is in geometric space (i.e., after inverting the canvas transform)
    */
   fun dragEnd(position: Vec2f) {}

   /**
    * The scale of gizmos in canvas space.
    * Controlled by the user.
    */
   val gizmoSize get() = Settings.gizmoSize.value
}

data class TransformedGizmo(val gizmo: Gizmo2D, val transform: Transform2D) {
   val collider get() = TransformedCollider2D(gizmo.collider, transform)
   fun render(canvas: BufferCanvas) = gizmo.render(canvas, transform)
   fun dragStart(pos: Vec2f) = gizmo.dragStart(transform.inverse.transform(pos))
   fun drag(pos: Vec2f) = gizmo.drag(transform.inverse.transform(pos))
   fun dragEnd(pos: Vec2f) = gizmo.dragEnd(transform.inverse.transform(pos))
}

/**
 * A simple gizmo that doesn't provide any interaction.
 *
 * Used to complement other gizmos, e.g. by rendering guiding lines.
 */
class DrawGizmo(val renderer: PixelRendererContext.(Transform2D) -> Unit) : Gizmo2D {
   override val collider: Collider2D get() = Collider2D.Empty
   override fun render(canvas: BufferCanvas, transform: Transform2D) {
      CanvasPixelRendererContext(canvas).apply {
         renderer(transform)
      }
   }
   override fun drag(position: Vec2f) {}
}

/**
 * Simple gizmo used to move a point in geometric space.
 */
class ControlPointGizmo(
   val value: () -> Vec2f, val style: Style = Style(),
   val onChange: GizmoDragListener
): Gizmo2D {
   override val collider get() = Point2fCollider(value())

   override fun render(canvas: BufferCanvas, transform: Transform2D) {
      with (CanvasPixelRendererContext(canvas)) {
         val pos = (transform * value()).toVec2i()
         renderPoint(Point2i(pos, style.outPoint))
         renderPoint(Point2i(pos, style.inPoint))
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

   data class Style(
      val outPoint: PointStyle = PointStyle(Color.LIGHT_GRAY, Settings.gizmoSize.value, CircleAntiAliasPointRenderer),
      val inPoint: PointStyle = outPoint.copy(color=Color.DARK_GRAY, size=outPoint.size-4F)
   ) {
      constructor(
         color: Color, renderer: Point2DRenderer = CircleAntiAliasPointRenderer, scale: Float = 1F
      ) : this(
         PointStyle(color, scale * Settings.gizmoSize.value, renderer),
         PointStyle(color.mix(Color.DARK_GRAY, 0.8F).withAlpha(0.2F), maxOf(0F, scale * Settings.gizmoSize.value - 4F), renderer))
   }
}

/**
 * A listener for drag events on a gizmo.
 */
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
