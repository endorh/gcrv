package endorh.unican.gcrv.animation

import endorh.unican.gcrv.scene.property.AnimProperty
import endorh.unican.gcrv.scene.property.AnimPropertyData
import endorh.unican.gcrv.scene.property.FloatProperty
import endorh.unican.gcrv.serialization.JsonFormat
import endorh.unican.gcrv.serialization.Vec2f
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test

class EasingSerializationTest {
   @Test
   fun testEasingSerialization() {
      val bezier = Easing.CubicBezier(Vec2f(0.2F, 0F), Vec2f(0.2F, 1F))
      testPrint(Easing.CubicBezier.serializer(), bezier)
      testPrint(Easing.serializer(), bezier)
   }

   @Test
   fun testKeyframeSerialization() {
      val bezier = Easing.CubicBezier(Vec2f(0.2F, 0F), Vec2f(0.2F, 1F))
      val keyframe = KeyFrame(TimeStamp.fromSeconds(1F), 0F, FloatProperty.LinearDriver, bezier)
      val serializer = KeyFrame.Serializer(Float.serializer(), listOf(FloatProperty.LinearDriver), true)
      testPrint(serializer, keyframe)
   }

   @Test
   fun testKeyframeListSerialization() {
      val bezier = Easing.CubicBezier(Vec2f(0.2F, 0F), Vec2f(0.2F, 1F))
      val keyframe = KeyFrame(TimeStamp.fromSeconds(1F), 0F, FloatProperty.LinearDriver, bezier)
      val keyframeList = KeyFrameList<Float>()
      keyframeList.set(keyframe)
      val listSerializer = KeyFrameList.Serializer(Float.serializer(), listOf(FloatProperty.LinearDriver))
      testPrint(listSerializer, keyframeList)
   }

   @Test
   fun testAnimDataSerialization() {
      val bezier = Easing.CubicBezier(Vec2f(0.2F, 0F), Vec2f(0.2F, 1F))
      val keyframe = KeyFrame(TimeStamp.fromSeconds(1F), 0F, FloatProperty.LinearDriver, bezier)
      val keyframeList = KeyFrameList<Float>()
      keyframeList.set(keyframe)
      val data = AnimPropertyData(keyFrames = keyframeList)

      val keyframeSerializer = KeyFrame.Serializer(Float.serializer(), listOf(FloatProperty.LinearDriver), true)
      val listSerializer = KeyFrameList.Serializer(Float.serializer(), listOf(FloatProperty.LinearDriver))
      val dataSerializer = AnimPropertyData.ConciseJsonSerializer(Float.serializer(), listOf(FloatProperty.LinearDriver))

      println("CubicBezier: " + JsonFormat.encodeToJsonElement(Easing.CubicBezier.serializer(), bezier))
      println("Easing: " + JsonFormat.encodeToJsonElement(Easing.serializer(), bezier))
      println("Keyframe: " + JsonFormat.encodeToJsonElement(keyframeSerializer, keyframe))
      println("KeyframeList: " + JsonFormat.encodeToJsonElement(listSerializer, keyframeList))
      println("AnimData: " + JsonFormat.encodeToJsonElement(dataSerializer, data))

      println("CubicBezier")
      testPrint(Easing.CubicBezier.serializer(), bezier)
      println("Easing")
      testPrint(Easing.serializer(), bezier)
      println("Keyframe")
      testPrint(keyframeSerializer, keyframe)
      println("KeyframeList")
      testPrint(listSerializer, keyframeList)
      println("AnimData")
      testPrint(dataSerializer, data)
   }

   fun <T> testPrint(serializer: KSerializer<T>, value: T) {
      println(JsonFormat.encodeToString(serializer, value))
   }
}