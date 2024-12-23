package endorh.unican.gcrv.scene

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.animation.TimeLine
import endorh.unican.gcrv.renderers.*
import endorh.unican.gcrv.scene.objects.*
import endorh.unican.gcrv.scene.property.*
import endorh.unican.gcrv.serialization.SavableSerializer
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.transformations.TransformProperty
import endorh.unican.gcrv.ui2.MutableSerialStateList
import endorh.unican.gcrv.util.MutableStateConcurrentList
import endorh.unican.gcrv.util.mutableStateConcurrentList
import endorh.unican.gcrv.util.toTitleCase
import endorh.unican.gcrv.util.toVec2f
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

@Serializable(Object2DStack.Serializer::class)
class Object2DStack {
   val objects: MutableStateConcurrentList<Object2D> = mutableStateConcurrentList()

   fun collectColliders(filteredObjects: Collection<Object2D>? = null, ignoreTransforms: Boolean = false, transform: Transform2D? = null): List<Pair<TransformedCollider2D, Object2D>> {
      val collector = TaggedColliderPassInputScopeImpl<Object2D>()
      transform?.let { collector.push(it) }
      fun render(o: Object2D) {
         if (!ignoreTransforms) collector.push(o.aggregatedTransform)
         if (filteredObjects == null || o in filteredObjects)
            collector.accept(o.collider to o)
         for (child in o.children) render(child)
         if (!ignoreTransforms) collector.pop()
      }
      for (o in objects.snapshot) render(o)
      return collector.collected
   }

   fun collectGizmos(selected: Collection<Object2D>? = null, ignoreTransforms: Boolean = false, transform: Transform2D? = null): List<TransformedGizmo> {
      val collector = TransformedGizmoPassInputScopeImpl()
      transform?.let { collector.push(it) }
      fun render(o: Object2D) {
         if (!ignoreTransforms) collector.push(o.aggregatedTransform)
         if (selected == null || o in selected)
            collector.accept(o.gizmos)
         for (child in o.children) render(child)
         if (!ignoreTransforms) collector.pop()
      }
      for (o in objects.snapshot) render(o)
      return collector.collected
   }

   object Serializer : KSerializer<Object2DStack> {
      val listSerializer = ListSerializer(PolymorphicSerializer(Object2D::class))
      override val descriptor get() = listSerializer.descriptor
      override fun deserialize(decoder: Decoder) = Object2DStack().apply {
         objects.addAll(listSerializer.deserialize(decoder))
      }
      override fun serialize(encoder: Encoder, value: Object2DStack) {
         listSerializer.serialize(encoder, value.objects)
      }
   }
}

val Object2DTypes = listOf(
   PointObject2D.Type,
   LineObject2D.Type,
   CubicSplineObject2D.Type,
   TriangleObject2D.Type,
   PolygonObject2D.Type,
   PolyLineObject2D.Type,
   GroupObject2D.Type,
)

@Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
@Serializable(PolymorphicSerializer::class)
@Polymorphic
abstract class Object2D(val type: Object2DType<out Object2D>) : PropertyHolder, SavableSerializer<Object2D.ObjectData> {
   private val geometricProperties = mutableListOf<GeometricProperty2D>()
   val geometry: List<GeometricProperty2D> get() = geometricProperties
   override val properties = PropertyMap()
   override val timeLine = mutableStateOf<TimeLine?>(null)
   open val gizmos: List<Gizmo2D> get() = emptyList()

   var name: String by string(type.generateName()).unique().static() priority 100
   var collapseChildren: Boolean by bool(false) priority 100 internal true

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
   open val collider: Collider2D get() = Collider2D.Empty

   @Suppress("UNCHECKED_CAST")
   operator fun <T> get(prop: KProperty<T>) = properties[prop.name] as? AnimProperty<T>
      ?: throw IllegalArgumentException("Property ${prop.name} is not an ObjectProperty")

   open fun onPropertyChange(block: () -> Unit) {
      properties.values.forEach { it.onChange { block() } }
      children.forEach { it.onPropertyChange(block) }
   }

   open fun setTimeLine(timeLine: TimeLine) {
      this.timeLine.value = timeLine
      children.forEach { it.setTimeLine(timeLine) }
   }

   protected operator fun <N: PropertyNode<*>> N.provideDelegate(thisRef: Object2D, property: KProperty<*>): N = apply {
      if (property.name in RESERVED_NAMES) throw IllegalArgumentException("Property name '${property.name}' is reserved!")
      init(property.name, thisRef)
      thisRef.register(this)
   }

   protected fun geometry(v: Vec2i) = geometry(v.toVec2f())
   protected fun geometry(v: Vec2f) = Vec2fProperty(v).also {
      it priority 10
      geometricProperties.add(it)
   }
   protected fun geoList(vararg values: Vec2f, showExpanded: Boolean = true, factory: () -> Vec2f = { Vec2f.ZERO }) =
      PropertyList({ Vec2fProperty(factory()) }, values.map { Vec2fProperty(it) }, showExpanded).also {
         geometricProperties.add(it)
      }

   protected fun gizmo(
      prop: KMutableProperty0<Vec2f>, style: ControlPointGizmo.Style = ControlPointGizmo.Style(),
      onDrag: GizmoDragListener? = null
   ) = ControlPointGizmo(prop::get, style, GizmoDragListener { pos ->
      prop.set(pos)
   }.redirect(onDrag))
   protected fun gizmo(
      prop: KProperty0<MutableList<Vec2f>>, i: Int, style: ControlPointGizmo.Style = ControlPointGizmo.Style(),
      onDrag: GizmoDragListener? = null
   ) = ControlPointGizmo({ prop.get()[i] }, style, GizmoDragListener {
      prop.get()[i] = it
   }.redirect(onDrag))
   protected fun gizmo(
      prop: Vec2fProperty, style: ControlPointGizmo.Style = ControlPointGizmo.Style(),
      onDrag: GizmoDragListener? = null
   ) = ControlPointGizmo({ prop.value }, style, GizmoDragListener {
      prop.value = it
   }.redirect(onDrag))
   protected fun drawGizmo(renderer: PixelRendererContext.(Transform2D) -> Unit) = DrawGizmo(renderer)

   open fun use(surface: UiSurface) {
      properties.allProperties.forEach { it.use(surface) }
   }
   open fun use(surface: UiSurface, prop: KProperty<*>) {
      when (val p = properties[prop.name]) {
         is AnimProperty<*> -> p.use(surface)
         is CompoundAnimProperty -> p.allProperties.forEach { it.use(surface) }
         is PropertyList<*, *> -> p.allProperties.forEach { it.use(surface) }
         null -> Unit
      }
   }
   fun UiScope.use() = use(surface)
   fun UiScope.use(prop: KProperty<*>) = use(surface, prop)

   override fun save() = ObjectData(children, properties)
   override fun load(data: ObjectData) {
      if (data.children.isNotEmpty()) {
         (children as? MutableList<Object2D>)?.let {
            it.clear()
            it.addAll(data.children)
         } ?: throw SerializationException("Object of type ${type.name} cannot have children!")
      } else if (children.isNotEmpty()) (children as? MutableList<Object2D>)?.clear()
      properties.load(data.properties)
   }
   override val saveSerializer by lazy {
      object : KSerializer<ObjectData> {
         override val descriptor = buildClassSerialDescriptor(type.name) {
            element("children", ListSerializer(PolymorphicSerializer(Object2D::class)).descriptor, isOptional = true)
            with(properties) {
               buildClassSerialDescriptor()
            }
         }
         override fun serialize(encoder: Encoder, value: ObjectData) = encoder.encodeStructure(descriptor) {
            if (value.children.isNotEmpty())
               encodeSerializableElement(descriptor, 0, ListSerializer(serializer()), value.children)
            with (properties) {
               encodeProperties(descriptor, 1, value.properties)
            }
         }
         override fun deserialize(decoder: Decoder): ObjectData = decoder.decodeStructure(descriptor) {
            var children: List<Object2D>? = null
            val properties = with (properties) {
               decodeProperties(descriptor, 1) {
                  when (it) {
                     0 -> children = decodeSerializableElement(descriptor, 0, ListSerializer(serializer()), children)
                     else -> throw SerializationException("Unexpected serial index: $it")
                  }
               }
            }
            ObjectData(children ?: emptyList(), properties ?: PropertyMap())
         }
      }
   }

   data class ObjectData(val children: List<Object2D>, val properties: PropertyMap)

   companion object {
      private val RESERVED_NAMES = setOf("children", "properties", "type")
   }
}

fun <T: Object2D> T.copy(): T {
   val data = save()
   return type.create().apply { load(data) } as T
}

interface Renderer2D {
   fun <D> RenderPassInputScope<D>.render() = when(this) {
      is WireframeRenderPassInputScope -> renderWireframe()
      is PointRenderPassInputScope -> renderPoints()
      is CubicSplineRenderPassInputScope -> renderCubicSplines()
      is PolyFillRenderPassInputScope -> renderPolyFills()
      else -> Unit
   }

   fun WireframeRenderPassInputScope.renderWireframe() {}
   fun PointRenderPassInputScope.renderPoints() {}
   fun CubicSplineRenderPassInputScope.renderCubicSplines() {}
   fun PolyFillRenderPassInputScope.renderPolyFills() {}
   object None : Renderer2D
}

abstract class Object2DType<T : Object2D>(val name: String) : KSerializer<T>, PresentableObject {
   override val displayName = name.toTitleCase()

   open fun generateName() = name
   abstract fun create(): T
   open fun createDrawerContext() = objectDrawer?.let {
      SimpleObjectDrawingContext(create(), it)
   }

   open val objectDrawer: ObjectDrawer<T>? = null

   val serializer: KSerializer<Object2D.ObjectData> by lazy {
      create().saveSerializer
   }
   override val descriptor: SerialDescriptor by lazy {
      serializer.descriptor
   }
   override fun serialize(encoder: Encoder, value: T) =
      encoder.encodeSerializableValue(/*Object2D.ConciseJsonSerializer*/(value.saveSerializer), value.save())
   override fun deserialize(decoder: Decoder) = create().apply {
      load(decoder.decodeSerializableValue(/*Object2D.ConciseJsonSerializer*/(saveSerializer)))
   }
}

interface ObjectDrawingContext<T: Object2D> {
   val drawnObject: T
   fun finish()
   fun update()
}

class SimpleObjectDrawingContext<T: Object2D>(
   override val drawnObject: T, val drawer: ObjectDrawer<T>
): ObjectDrawingContext<T> {
   var isFinished: Boolean = false
      private set
   var onUpdate: (() -> Unit)? = null
   override fun finish() {
      isFinished = true
   }
   override fun update() {
      onUpdate?.invoke()
   }

   init { with (drawer) { init() } }

   fun dragStart(pos: Vec2f) = with(drawer) { onDragStart(pos) }
   fun drag(pos: Vec2f) = with(drawer) { onDrag(pos) }
   fun hover(pos: Vec2f) = with(drawer) { onHover(pos) }
   fun dragEnd(pos: Vec2f) = with(drawer) { onDragEnd(pos) }
   fun rightClick() = with(drawer) { onRightClick() }
   fun UiScope.drawerStyleEditor() = with(drawer) { styleEditor(drawnObject) }
}

/**
 * Used to handle object drawing events.
 *
 * - [init] is called when an object drawing operation starts with a fresh new object.
 * - [onDragStart] is called when clicked down. [onDrag] is guaranteed to be called right after.
 * - [onDrag] is called whenever the mouse moves while clicked down.
 * - [onDragEnd] is called when the mouse stops being clicked. [onDrag] is guaranteed to have been
 *   called before with the same `pos` parameter.
 * - [onHover] is called whenever the mouse moves while not held.
 * - [onRightClick] is called whenever the user performs a right click
 */
interface ObjectDrawer<T: Object2D> {
   fun ObjectDrawingContext<T>.init() {}
   fun ObjectDrawingContext<T>.onDragStart(pos: Vec2f) {}
   fun ObjectDrawingContext<T>.onDrag(pos: Vec2f) {}
   fun ObjectDrawingContext<T>.onHover(pos: Vec2f) {}
   fun ObjectDrawingContext<T>.onDragEnd(pos: Vec2f) {}
   fun ObjectDrawingContext<T>.onRightClick() {}

   fun UiScope.styleEditor(obj: T?) {}
}
