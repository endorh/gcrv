package endorh.unican.gcrv.types

import endorh.unican.gcrv.animation.Easing
import endorh.unican.gcrv.animation.EasingType
import endorh.unican.gcrv.animation.EasingTypes
import endorh.unican.gcrv.line_algorithms.Line2DRenderer
import endorh.unican.gcrv.line_algorithms.Point2DRenderer
import endorh.unican.gcrv.line_algorithms.renderers.LineRenderers
import endorh.unican.gcrv.line_algorithms.renderers.PointRenderers
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
val serializersModule = SerializersModule {
   polymorphic(Easing::class) {
      @Suppress("UNCHECKED_CAST")
      fun <T: Easing> PolymorphicModuleBuilder<Easing>.subEasing(type: EasingType<T>) =
         subclass(type.factory()::class as KClass<T>, type)
      for (type in EasingTypes) subEasing(type)
   }

   polymorphic(Line2DRenderer::class) {
      @Suppress("UNCHECKED_CAST")
      for (renderer in LineRenderers)
         subclass(renderer::class as KClass<Line2DRenderer>, renderer::class.serializer() as KSerializer<Line2DRenderer>)
   }

   polymorphic(Point2DRenderer::class) {
      @Suppress("UNCHECKED_CAST")
      for (renderer in PointRenderers)
         subclass(renderer::class as KClass<Point2DRenderer>, renderer::class.serializer() as KSerializer<Point2DRenderer>)
   }
}