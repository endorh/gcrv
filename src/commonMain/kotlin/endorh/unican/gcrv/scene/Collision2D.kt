package endorh.unican.gcrv.scene

import de.fabmax.kool.demo.Settings
import de.fabmax.kool.math.Vec2f
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.util.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Describes a collision region in 2D space, mainly for object picking in a canvas.
 *
 * Collision regions for canvas object picking may depend on the canvas transform,
 * in order to provide canvas-space collision sizes.
 *
 * Colliders can be collected by the [Object2DStack.collectColliders] method, which
 * wraps colliders for objects in an [Object2DStack] as [TransformedCollider2D],
 * according to the current canvas transform.
 */
interface Collider2D {
   /**
    * Bounding rectangle for this collider in geometric space.
    */
   val rectangle: Rect2f

   /**
    * Center of this collider in geometric space, used to select the closest
    * collider to an input position.
    *
    * It may not correspond to the actual geometric center of the collider.
    */
   val center: Vec2f get() = rectangle.center

   /**
    * Performs a bounding box test to quickly determine if a position [p] should be tested
    * further for collision.
    *
    * The [canvasTransform] is used to determine the tolerance given by [gizmoSize] in the test.
    */
   fun fastContains(p: Vec2f, canvasTransform: Transform2D): Boolean {
      val pp = rectangle.clamp(p)
      var d = p - pp
      if (d.x == 0F && d.y == 0F)
         return contains(p, canvasTransform)
      d = canvasTransform * d
      val delta = max(abs(d.x), abs(d.y))
      return delta <= gizmoSize && contains(p, canvasTransform)
   }

   /**
    * Determine if a position [p] in geometric space is contained within this collider.
    *
    * The [canvasTransform] is necessary to measure distances in canvas space
    * `(1 unit = 1 px)`, such as gizmo sizes.
    */
   fun contains(p: Vec2f, canvasTransform: Transform2D): Boolean

   /**
    * Determine if a position `p` in canvas space is contained within this collider.
    */
   operator fun contains(p: Vec2f) = contains(p, Transform2D.identity)

   /**
    * Distance to the center of this collider, used to select the closest collider to
    * an input position.
    *
    * In some cases, the returned value may be smaller than the actual distance to the
    * [center] property.
    * For example, [UnionCollider2D]s return the minimum distance to any of their centers,
    * to allow fair selection of the closest object from the user's perspective.
    */
   fun centerDistance(p: Vec2f): Float = center.distance(p)

   /**
    * Dimension of this collider in geometric space.
    *
    * Used to prefer selecting objects of smaller dimension when they overlap.
    *
    * For non-empty 2D colliders, this value should naturally be between `0` and `2`.
    * Empty colliders should have dimension `-1`.
    */
   val dimension: Int

   /**
    * Area of this collider in geometric space.
    *
    * Used to prefer selecting objects of smaller area when they overlap.
    *
    * Colliders such as [LineSegment2f] that describe objects of `co-dimension > 0` should
    * have area `0`, even if their actual collision region does not.
    */
   val area: Float get() = 0F

   /**
    * The empty collider.
    *
    * Has dimension `-1`, and doesn't contain any position.
    * The [rectangle] and [center] properties are both [Rect2f.ZERO] and [Vec2f.ZERO],
    * which shouldn't be relied upon.
    * The [centerDistance] is always `+∞`.
    */
   object Empty : Collider2D {
      override val rectangle get() = Rect2f.ZERO
      override val dimension get() = -1

      override fun fastContains(p: Vec2f, canvasTransform: Transform2D) = false
      override fun contains(p: Vec2f, canvasTransform: Transform2D) = false
      override fun centerDistance(p: Vec2f) = Float.POSITIVE_INFINITY
   }

   /**
    * Scale used by gizmos in canvas space.
    *
    * Controlled by the user.
    */
   val gizmoSize get() = Settings.gizmoSize.value
}

/**
 * A collider coupled with a canvas transform.
 */
class TransformedCollider2D(val collider: Collider2D, val transform: Transform2D) {
   val dimension get() = collider.dimension
   val area get() = collider.area
   val center: Vec2f get() = transform.transform(collider.center)

   operator fun contains(p: Vec2f) = collider.fastContains(transform.inverse.transform(p), transform)
   fun centerDistance(pos: Vec2f) = collider.centerDistance(transform.inverse.transform(pos))
}

/**
 * Collision region with a rectangle axis-aligned in geometric space.
 */
class Rect2fCollider(override val rectangle: Rect2f) : Collider2D {
   override val dimension get() = 2
   override val area get() = rectangle.area

   override fun fastContains(p: Vec2f, canvasTransform: Transform2D) = contains(p, canvasTransform)
   override fun contains(p: Vec2f, canvasTransform: Transform2D): Boolean = rectangle.containsWithMargin(p, gizmoSize)
}

/**
 * Collision region with a line segment.
 */
class LineSegment2fCollider(val line: LineSegment2f) : Collider2D {
   override val dimension get() = 1
   override val center = (line.start + line.end) / 2F
   val direction = line.direction
   val normal = line.normal
   val cut = normal * line.start
   val startThreshold = direction * line.start
   val endThreshold = direction * line.end

   val div by lazy { line.length }
   val divSquare by lazy { line.lengthSquare }

   val radius get() = gizmoSize / 2F

   override val rectangle by lazy {
      Rect2f(
         minOf(line.start.x, line.end.x),
         minOf(line.start.y, line.end.y),
         maxOf(line.start.x, line.end.x),
         maxOf(line.start.y, line.end.y))
   }

   override fun contains(p: Vec2f, canvasTransform: Transform2D): Boolean {
      val delta = (p - line.start)
      val normalDelta = canvasTransform linearTransform (normal * ((normal * delta) / divSquare))
      if (normalDelta.sqrLength() > radius*radius)
         return false
      val test = direction * p
      val ref = if (test < startThreshold) {
         line.start
      } else if (test > endThreshold) {
         line.end
      } else return true
      return (canvasTransform linearTransform (p - ref)).sqrLength() < radius*radius
   }

   override fun centerDistance(p: Vec2f) = center.distance(p)
}

/**
 * Collision region given by a polygon's vertices.
 *
 * Current implementation only supports convex polygons.
 */
class Polygon2fCollider(val points: List<Vec2f>) : Collider2D {
   override val dimension get() = 2
   override val center = points.reduce(Vec2f::plus) / points.size.F
   val radius get() = gizmoSize
   val semiPlanes = if (points.size <= 1) emptyList() else (points + points.first()).windowed(2, 1, false) {
      val dir = it[1] - it[0]
      var normal = Vec2f(-dir.y, dir.x)
      if (normal.dot(center) < normal.dot(it[0]))
         normal = -normal
      SemiPlane2f(normal, it[0])
   }
   override val rectangle = points.boundingBox()
   override fun contains(p: Vec2f, canvasTransform: Transform2D) = semiPlanes.all {
      it.contains(p) || (canvasTransform linearTransform it.normalTo(p)).sqrLength() < radius*radius
   }
   override val area: Float get() {
      // We assume convexity
      if (points.size <= 2) return 0F
      var area = 0F
      val p0 = points[0]
      for (i in 1 until points.size - 1) {
         val p1 = points[i]
         val p2 = points[i+1]
         area += (p1 - p0).cross(p2 - p0)
      }
      return area / 2F
   }
}

/**
 * Collider for a point.
 *
 * The interactable radius depends on the [gizmoSize].
 */
class Point2fCollider(val point: Vec2f) : Collider2D {
   override val dimension get() = 0
   val radius get() = gizmoSize
   val sqrRadius get() = radius*radius
   override val rectangle by lazy {
      Rect2f(point.x, point.y, point.x, point.y)
   }
   override val center get() = point

   // Distance testing is likely faster than all the branching involved in a rectangle test,
   // with the added overhead of creating the rectangle
   override fun fastContains(p: Vec2f, canvasTransform: Transform2D) =
      contains(p, canvasTransform)
   override fun contains(p: Vec2f, canvasTransform: Transform2D) =
      (canvasTransform linearTransform (p - point)).sqrLength() < sqrRadius
   override fun centerDistance(p: Vec2f) = point.distance(p)
}

/**
 * Union of multiple colliders as one.
 *
 * Can be used to create complex shapes.
 */
class UnionCollider2D(val colliders: List<Collider2D>) : Collider2D {
   override val dimension by lazy { colliders.maxOf { it.dimension } }
   override val rectangle by lazy {
      colliders.map { it.rectangle }.reduce(Rect2f::unionRect)
   }
   override val center by lazy {
      colliders.map { it.center }.reduce { acc, c -> acc + c } / colliders.size.F
   }
   override val area by lazy {
      colliders.filter { it.dimension == dimension }.sumOf { it.area.D }.F
   }

   override fun centerDistance(p: Vec2f) = colliders.minOf { it.centerDistance(p) }

   override fun fastContains(p: Vec2f, canvasTransform: Transform2D) = colliders.any { it.fastContains(p, canvasTransform) }
   override fun contains(p: Vec2f, canvasTransform: Transform2D) = colliders.any { it.contains(p, canvasTransform) }
}