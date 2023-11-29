package endorh.unican.gcrv.objects

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.line_algorithms.*
import endorh.unican.gcrv.objects.property.*
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.transformations.TransformProperty
import endorh.unican.gcrv.util.div
import endorh.unican.gcrv.util.plus
import endorh.unican.gcrv.util.toVec2f
import endorh.unican.gcrv.util.toVec2i
import kotlin.reflect.KProperty

class Object2DStack {
   val objects = mutableStateListOf<Object2D>()
}

abstract class Object2D(val type: Object2DType<out Object2D> = Object2DType("Object")) : PropertyHolder {
   private val geometricProperties = mutableListOf<Vec2fProperty>()
   val geometry: List<Vec2fProperty> get() = geometricProperties
   override val properties = PropertyMap()
   override val timeLine = mutableStateOf<TimeLine?>(null)

   var name: String by string(type.generateName()).unique().static() priority 100

   open val children: List<Object2D> get() = emptyList()
   open val renderers: List<Renderer2D> = emptyList()

   val localTransforms by list { TransformProperty() } priority -10
   val globalTransforms by list { TransformProperty() } priority -10

   val aggregatedTransform get() =
      globalTransforms.entries
         .map { it.transform }
         .fold(Transform2D.identity) { acc, t -> t * acc } *
      localTransforms.entries
         .map { it.transform.localize(geometricCenter) }
         .fold(Transform2D.identity) { acc, t -> t * acc }
   open val geometricCenter: Vec2f get() = Vec2f.ZERO

   @Suppress("UNCHECKED_CAST")
   operator fun <T> get(prop: KProperty<T>) = properties[prop.name] as? AnimProperty<T>
      ?: throw IllegalArgumentException("Property ${prop.name} is not an ObjectProperty")

   fun onPropertyChange(block: () -> Unit) = properties.values.forEach { it.onChange { block() } }

   protected operator fun <N: PropertyNode> N.provideDelegate(thisRef: Object2D, property: KProperty<*>): N = apply {
      init(property.name, thisRef)
      thisRef.register(this)
   }

   protected fun geometry(v: Vec2i) = geometry(v.toVec2f())
   protected fun geometry(v: Vec2f) = Vec2fProperty(v).also {
      it priority 10
      geometricProperties.add(it)
   }

   fun use(surface: UiSurface) {
      properties.allProperties.forEach { it.use(surface) }
   }
   fun use(surface: UiSurface, prop: KProperty<*>) {
      when (val p = properties[prop.name]) {
         is AnimProperty<*> -> p.use(surface)
         is CompoundAnimProperty -> p.allProperties.forEach { it.use(surface) }
         is PropertyList<*> -> p.allProperties.forEach { it.use(surface) }
         null -> Unit
      }
   }
   fun UiScope.use() = use(surface)
   fun UiScope.use(prop: KProperty<*>) = use(surface, prop)
}

interface Renderer2D {
   fun <D> RenderPassInputScope<D>.render() = when(this) {
      is WireframeRenderPassInputScope -> renderWireframe()
      is PointRenderPassInputScope -> renderPoints()
      else -> Unit
   }

   fun WireframeRenderPassInputScope.renderWireframe() {}
   fun PointRenderPassInputScope.renderPoints() {}
   object None : Renderer2D
}

open class Object2DType<T : Object2D>(val name: String) {
   open fun generateName() = name
}

class GroupObject2D : Object2D(Type) {
   override val children = mutableStateListOf<Object2D>()

   override val geometricCenter: Vec2f get() {
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

   companion object Type : Object2DType<GroupObject2D>("Group") {
      private var groupCount = 0
      override fun generateName() = "Group ${++groupCount}"
   }
}

class PointObject2D(pos: Vec2i, style: PointStyle = PointStyle(Color.WHITE, 7F)) : Object2D(Type) {
   var pos by geometry(pos)

   val style by pointStyle(style) priority -20

   override val geometricCenter: Vec2f get() = pos

   override val renderers: List<Renderer2D> = listOf(Renderer(this))

   class Renderer(val point: PointObject2D) : Renderer2D {
      override fun PointRenderPassInputScope.renderPoints() {
         accept(Point2D(point.pos.toVec2i(), point.style.pointStyle))
      }
   }

   object Type : Object2DType<PointObject2D>("Point") {
      private var pointCount = 0
      override fun generateName() = "Point ${++pointCount}"
   }
}

class LineObject2D(
   start: Vec2i, end: Vec2i,
   style: LineStyle = LineStyle(Color.WHITE),
   startStyle: PointStyle = PointStyle(Color.WHITE, 5F),
   endStyle: PointStyle = PointStyle(Color.WHITE, 5F),
) : Object2D(Type) {
   var start by geometry(start)
   var end by geometry(end)

   val style by lineStyle(style, startStyle, endStyle) priority -20

   override val geometricCenter: Vec2f get() = (start + end) / 2F
   override val renderers: List<Renderer2D> = listOf(Renderer(this))

   class Renderer(val line: LineObject2D) : Renderer2D {
      override fun WireframeRenderPassInputScope.renderWireframe() {
         accept(Line2D(line.start.toVec2i(), line.end.toVec2i(), line.style.lineStyle))
      }
      override fun PointRenderPassInputScope.renderPoints() {
         accept(Point2D(line.start.toVec2i(), line.style.start.pointStyle), Point2D(line.end.toVec2i(), line.style.end.pointStyle))
      }
   }

   object Type : Object2DType<LineObject2D>("Line") {
      private var lineCount = 0
      override fun generateName() = "Line ${++lineCount}"
   }
}