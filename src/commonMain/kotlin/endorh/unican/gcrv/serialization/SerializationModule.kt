package endorh.unican.gcrv.serialization

import endorh.unican.gcrv.animation.Easing
import endorh.unican.gcrv.animation.EasingType
import endorh.unican.gcrv.animation.EasingTypes
import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderer
import endorh.unican.gcrv.fractals.RecursiveGeoFractalRenderers
import endorh.unican.gcrv.renderers.CubicSplineRenderers
import endorh.unican.gcrv.renderers.LineRenderers
import endorh.unican.gcrv.renderers.PointRenderers
import endorh.unican.gcrv.renderers.PolyFillRenderers
import endorh.unican.gcrv.scene.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
val module = SerializersModule {
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

   polymorphic(CubicSpline2DRenderer::class) {
      @Suppress("UNCHECKED_CAST")
      for (renderer in CubicSplineRenderers)
         subclass(renderer::class as KClass<CubicSpline2DRenderer>, renderer::class.serializer() as KSerializer<CubicSpline2DRenderer>)
   }

   polymorphic(RecursiveGeoFractalRenderer::class) {
      @Suppress("UNCHECKED_CAST")
      for (renderer in RecursiveGeoFractalRenderers)
         subclass(renderer::class as KClass<RecursiveGeoFractalRenderer>, renderer::class.serializer() as KSerializer<RecursiveGeoFractalRenderer>)
   }

   polymorphic(PolyFill2DRenderer::class) {
      @Suppress("UNCHECKED_CAST")
      for (renderer in PolyFillRenderers)
         subclass(renderer::class as KClass<PolyFill2DRenderer>, renderer::class.serializer() as KSerializer<PolyFill2DRenderer>)
   }

   polymorphic(Object2D::class) {
      @Suppress("UNCHECKED_CAST")
      fun <T: Object2D> PolymorphicModuleBuilder<Object2D>.subType(type: Object2DType<T>) =
         subclass(type.create()::class as KClass<T>, type)
      for (type in Object2DTypes) subType(type)
   }
}

@OptIn(ExperimentalSerializationApi::class)
val JsonFormat = Json {
   serializersModule = module
   prettyPrint = true
   prettyPrintIndent = "  "
}