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
import endorh.unican.gcrv.util.*
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

data class PolyFillStyle(
   val color: Color = Color.GRAY,
   val renderer: PolyFill2DRenderer? = null
)

data class Point2f(val pos: Vec2f, val style: PointStyle = PointStyle()) {
   fun toPoint2i() = Point2i(pos.toVec2i(), style)
}
data class Point2i(val pos: Vec2i, val style: PointStyle = PointStyle()) {
   val x get() = pos.x
   val y get() = pos.y
}

data class LineSegment2f(val start: Vec2f, val end: Vec2f, val style: LineStyle = LineStyle()) {
   val dx get() = end.x - start.x
   val dy get() = end.y - start.y
   val direction get() = end - start
   val normal get() = Vec2f(-dy, dx)

   val c get() = dy * start.x - dx * start.y
   val lengthSquare get() = dx * dx + dy * dy
   val length get() = sqrt(dx * dx + dy * dy)

   val slope get() = dy / dx
   val invSlope get() = dx / dy
   val yIntercept get() = start.y - slope * start.x
   val xIntercept get() = -yIntercept / slope

   fun toLine2i() = LineSegment2i(start.toVec2i(), end.toVec2i(), style)
}
data class LineSegment2i(val start: Vec2i, val end: Vec2i, val style: LineStyle = LineStyle()) {
   val dx get() = end.x - start.x
   val dy get() = end.y - start.y
   val normal get() = Vec2i(-dy, dx)

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
   @JvmInline value class LeftToRightAccessor(val line: LineSegment2i) {
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
   @JvmInline value class ComponentsAccessor(val line: LineSegment2i) {
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
   @JvmInline value class LeftToRightComponentsAccessor(val line: LineSegment2i) {
      operator fun component1() = line.leftPoint.x
      operator fun component2() = line.leftPoint.y
      operator fun component3() = line.rightPoint.x
      operator fun component4() = line.rightPoint.y
   }
}

data class CubicSpline2f(
   val p0: Vec2f, val p1: Vec2f, val p2: Vec2f, val p3: Vec2f,
   val style: CubicSplineStyle = CubicSplineStyle()
) {
   fun transform(t: Transform2D) = CubicSpline2f(
      t * p0, t * p1, t * p2, t * p3, style)

   fun valueAt(t: Float, res: MutableVec2f = MutableVec2f()): MutableVec2f = res.apply {
      val tc = 1F - t
      val t2 = t*t
      val tc2 = tc*tc
      x = tc2*tc*p0.x + 3F*tc2*t*p1.x + 3F*tc*t2*p2.x + t2*t*p3.x
      y = tc2*tc*p0.y + 3F*tc2*t*p1.y + 3F*tc*t2*p2.y + t2*t*p3.y
   }
}

data class PolyFill2f(val points: List<Vec2f>, val style: PolyFillStyle) {
   val semiPlanes: List<SemiPlane2f> by lazy {
      if (points.size <= 2) return@lazy emptyList()
      val neg = (points + points.subList(0, 2)).windowed(3) {
         val (a, b, c) = it
         val ba = a - b
         val bc = c - b
         ba.cross(bc)
      }.sum() < 0F

      (points + listOf(points.first())).windowed(2) {
         val (a, b) = it
         val d = b - a
         val n = if (neg) Vec2f(-d.y, d.x) else Vec2f(d.y, -d.x)
         SemiPlane2f(n, a)
      }
   }
   val boundingBox by lazy { points.boundingBox() }

   fun toPolyFill2i() = PolyFill2i(points.map { it.toVec2i() }, style)
}

data class PolyFill2i(val points: List<Vec2i>, val style: PolyFillStyle)

interface Point2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(point: Point2i)
}

interface Line2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(line: LineSegment2i)
}

interface CubicSpline2DRenderer : PresentableObject {
   val name: String
   override val displayName get() = name
   fun PixelRendererContext.render(spline: CubicSpline2f)
}

interface PolyFill2DRenderer : PresentableObject {
   val name: String
   override val displayName: String get() = name
   fun PixelRendererContext.render(fill: PolyFill2f)
}

interface PixelRendererContext {
   fun withColor(color: Color, block: () -> Unit)
   fun plotPixel(x: Int, y: Int)
   fun plotPixel(x: Int, y: Int, alpha: Float) = plotPixel(x, y)
}

fun PixelRendererContext.renderPoint(point: Point2i) {
   with (point.style.renderer ?: CircleAntiAliasPointRenderer) {
      withColor(point.style.color) {
         render(point)
      }
   }
}
fun PixelRendererContext.renderLine(line: LineSegment2i) {
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

data class SemiPlane2f(val normal: Vec2f, val point: Vec2f) {
   val cut by lazy { normal.dot(point) }
   operator fun contains(p: Vec2f) = normal.dot(p) - cut >= 0
   operator fun contains(p: Vec2i) = normal.x * p.x + normal.y * p.y - cut >= 0

   fun normalTo(p: Vec2f) = normal * (normal * (p - point) / normal.sqrLength())
}

data class SemiPlane2i(val normal: Vec2i, val point: Vec2i) {
   val cut by lazy { normal.dot(point) }
   operator fun contains(p: Vec2i) = normal.dot(p) - cut >= 0
   operator fun contains(p: Vec2f) = normal.x * p.x + normal.y * p.y - cut >= 0

   fun normalTo(p: Vec2i) = normal.toVec2f() * (normal.dot(p - point) / (normal.x.squared + normal.y.squared).F)
}