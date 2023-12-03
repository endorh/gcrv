package endorh.unican.gcrv.scene

import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.renderers.PresentableObject
import endorh.unican.gcrv.renderers.line.BresenhamRendererBreadthAntiAlias
import endorh.unican.gcrv.renderers.point.CircleAntiAliasPointRenderer
import endorh.unican.gcrv.renderers.point.SquarePointRenderer
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.BufferCanvas
import endorh.unican.gcrv.util.F
import kotlin.jvm.JvmInline
import kotlin.math.sqrt

data class PointStyle(
   val color: Color = Color.WHITE,
   val size: Float = 1F,
   val renderer: Point2DRenderer? = null
)

data class LineStyle(
   val color: Color = Color.WHITE,
   val breadth: Float = 1F,
   val renderer: Line2DRenderer? = null
)

data class CubicSplineStyle(
   val color: Color = Color.WHITE,
   val breadth: Float = 1F,
   val renderer: CubicSpline2DRenderer? = null,
   val startStyle: PointStyle = PointStyle(Color.RED, 7F),
   val midStyle: PointStyle = PointStyle(Color.GREEN, 5F, SquarePointRenderer),
   val endStyle: PointStyle = PointStyle(Color.BLUE, 7F),
)

data class Point2D(val pos: Vec2i, val style: PointStyle = PointStyle()) {
   val x get() = pos.x
   val y get() = pos.y
}

data class Line2D(val start: Vec2i, val end: Vec2i, val style: LineStyle = LineStyle()) {
   val dx get() = end.x - start.x
   val dy get() = end.y - start.y

   val c get() = dy * start.x - dx * start.y
   val lengthSquare get() = dx*dx + dy*dy
   val length get() = sqrt((dx*dx + dy*dy).F)

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

data class CubicSpline2D(
   val p0: Vec2f, val p1: Vec2f, val p2: Vec2f, val p3: Vec2f,
   val style: CubicSplineStyle = CubicSplineStyle()
) {
   fun transform(t: Transform2D) = CubicSpline2D(
      t * p0, t * p1, t * p2, t * p3, style)

   fun valueAt(t: Float, res: MutableVec2f = MutableVec2f()): MutableVec2f = res.apply {
      val tc = 1F - t
      val t2 = t*t
      val tc2 = tc*tc
      x = tc2*tc*p0.x + 3F*tc2*t*p1.x + 3F*tc*t2*p2.x + t2*t*p3.x
      y = tc2*tc*p0.y + 3F*tc2*t*p1.y + 3F*tc*t2*p2.y + t2*t*p3.y
   }
}

interface Point2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(point: Point2D)
}

interface Line2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(line: Line2D)
}

interface CubicSpline2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(spline: CubicSpline2D)
}

interface PixelRendererContext {
   fun withColor(color: Color, block: () -> Unit)
   fun plotPixel(x: Int, y: Int)
   fun plotPixel(x: Int, y: Int, alpha: Float) = plotPixel(x, y)
}

fun PixelRendererContext.renderPoint(point: Point2D) {
   with (point.style.renderer ?: CircleAntiAliasPointRenderer) {
      withColor(point.style.color) {
         render(point)
      }
   }
}
fun PixelRendererContext.renderLine(line: Line2D) {
   with (line.style.renderer ?: BresenhamRendererBreadthAntiAlias) {
      withColor(line.style.color) {
         render(line)
      }
   }
}

class CanvasPixelRendererContext(val canvas: BufferCanvas) : PixelRendererContext {
   var color: Color = Color.WHITE
   override fun withColor(color: Color, block: () -> Unit) {
      val prev = this.color
      this.color = color
      block()
      this.color = prev
   }

   override fun plotPixel(x: Int, y: Int) {
      canvas.F.C.M[x, y] = color
   }

   override fun plotPixel(x: Int, y: Int, alpha: Float) {
      canvas.F.C.M[x, y] = color.withAlpha(alpha)
   }
}