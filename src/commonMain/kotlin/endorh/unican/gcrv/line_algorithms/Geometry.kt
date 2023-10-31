package endorh.unican.gcrv.line_algorithms

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.F
import kotlin.jvm.JvmInline

data class LineStyle(
   val color: Color,
   val breadth: Float = 1F,
   val renderer: Line2DRenderer? = null
)

data class Line2D(val start: Vec2i, val end: Vec2i, val style: LineStyle) {
   val dx get() = end.x - start.x
   val dy get() = end.y - start.y

   val slope get() = dy.F / dx.F
   val invSlope get() = dx.F / dy.F
   val yIntercept get() = start.y - slope * start.x
   val xIntercept get() = -yIntercept / slope

   val isVertical get() = start.x == end.x
   val isHorizontal get() = start.y == end.y
   val isPoint get() = start == end
   val isOrthogonal get() = isVertical || isHorizontal
   val is45Degree get() = slope == 1F || slope == -1F
   val isRasterFriendly get() = isOrthogonal || is45Degree

   val isSlopePositive get() = end.y > start.y
   val isSlopeNegative get() = end.y < start.y
   val isSlopeSmallerThanOne get() = dy < dx
   val isSlopeGreaterThanOne get() = dy > dx

   val isLeftward get() = start.x > end.x
   val isRightward get() = start.x < end.x
   // Standard mathematical upward notion
   val isUpward get() = start.y < end.y
   val isDownward get() = start.y > end.y

   val leftPoint get() = if (isRightward) start else end
   val rightPoint get() = if (isRightward) end else start
   val topPoint get() = if (isDownward) start else end
   val bottomPoint get() = if (isDownward) end else start

   /**
    * Shortcut to unpack endpoints in left-to-right order:
    * ```
    * val (left, right) = line.leftToRight
    * ```
    */
   inline val leftToRight get() = LeftToRightAccessor(this)
   @JvmInline value class LeftToRightAccessor(val line: Line2D) {
      operator fun component1() = line.leftPoint
      operator fun component2() = line.rightPoint
   }

   /**
    * Shortcut to unpack both endpoint coordinates:
    * ```
    * val (xS, yS, xE, yE) = line.coords
    * ```
    */
   inline val coords get() = ComponentsAccessor(this)
   @JvmInline value class ComponentsAccessor(val line: Line2D) {
      operator fun component1() = line.start.x
      operator fun component2() = line.start.y
      operator fun component3() = line.end.x
      operator fun component4() = line.end.y
   }

   /**
    * Shortcut to unpack both endpoint coordinates in left-to-right order:
    * ```
    * val (xL, yL, xR, yR) = line.leftToRightCoords
    * ```
    */
   inline val leftToRightCoords get() = LeftToRightComponentsAccessor(this)
   @JvmInline value class LeftToRightComponentsAccessor(val line: Line2D) {
      operator fun component1() = line.leftPoint.x
      operator fun component2() = line.leftPoint.y
      operator fun component3() = line.rightPoint.x
      operator fun component4() = line.rightPoint.y
   }
}

interface Line2DRenderer {
   val name: String
   fun PixelRendererContext.render(line: Line2D)
}

interface Point2DRenderer {
   val name: String
   fun PixelRendererContext.render(point: Vec2i, size: Int)
}

interface PixelRendererContext {
   fun plotPixel(x: Int, y: Int)
   fun plotPixel(x: Int, y: Int, alpha: Float) = plotPixel(x, y)
}

class CanvasPixelRendererContext(val canvas: BufferCanvas) : PixelRendererContext {
   var color: Color = Color.WHITE
   override fun plotPixel(x: Int, y: Int) {
      canvas.F.C.M[x, y] = color
   }

   override fun plotPixel(x: Int, y: Int, alpha: Float) {
      canvas.F.C.M[x, y] = color.withAlpha(alpha)
   }
}