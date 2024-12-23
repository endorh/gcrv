package endorh.unican.gcrv.fractals

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.TreeMap
import endorh.unican.gcrv.scene.PointStyle
import endorh.unican.gcrv.scene.objects.PointObject2D
import endorh.unican.gcrv.serialization.Color
import endorh.unican.gcrv.serialization.randomHue
import endorh.unican.gcrv.transformations.TaggedTransform2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.random.Random

class IFSFunctionDraft(
   transform: TaggedTransform2D = TaggedTransform2D.identity, weight: Float = 1F, color: Color = Color.LIGHT_GRAY
) {
   val transform = nonPreemptiveMutableStateOf(transform)
   val weight = nonPreemptiveMutableStateOf(weight)
   val color = nonPreemptiveMutableStateOf(color)
   private val callbacks = mutableListOf<() -> Unit>()
   private fun triggerChange() {
      callbacks.forEach { it() }
   }

   init {
      this.transform.onChange { triggerChange() }
      this.weight.onChange { triggerChange() }
      this.color.onChange { triggerChange() }
   }

   fun onChange(callback: () -> Unit) {
      callbacks += callback
   }
   fun removeChangeCallback(callback: () -> Unit) {
      callbacks -= callback
   }

   fun create() = IFSFunction(transform.value.toTransform(), weight.value, color.value)
   fun copy() = IFSFunctionDraft(transform.value, weight.value, color.value)

   companion object {
      fun from(f: IFSFunction) = IFSFunctionDraft(
         TaggedTransform2D.fromTransform(f.transform), f.weight, f.color)
   }
}

class IFSDraft(
   functions: List<IFSFunctionDraft> = listOf(IFSFunctionDraft()), seed: Int = 0
) {
   val functions = nonPreemptiveMutableStateOf(functions)
   val seed = nonPreemptiveMutableStateOf(seed)
   private val callbacks = mutableListOf<() -> Unit>()
   private fun triggerChange() {
      callbacks.forEach { it() }
   }
   init {
      this.functions.value.forEach {
         it.onChange { triggerChange() }
      }
      this.functions.onChange { fs ->
         fs.forEach { it.removeChangeCallback(::triggerChange) }
         this.functions.value.forEach { f -> f.onChange(::triggerChange) }
         triggerChange()
      }
      this.seed.onChange { triggerChange() }
   }
   fun onChange(callback: () -> Unit) {
      callbacks += callback
   }
   fun create() = IFS(functions.value.map { it.create() }, seed.value)
   fun load(ifs: IFS) {
      functions.value = ifs.functions.map { IFSFunctionDraft.from(it) }
      seed.value = ifs.seed
      triggerChange()
   }

   companion object {
      fun fromIFSFunctions(functions: List<IFSFunction>, seed: Int = 0) =
         IFSDraft(functions.map { IFSFunctionDraft.from(it) }, seed)
      fun fromIFS(ifs: IFS) = fromIFSFunctions(ifs.functions, ifs.seed)
   }
}

@Serializable
data class IFSFunction(val transform: Transform2D, val weight: Float, val color: Color)

@Serializable
class IFS(
   val functions: List<IFSFunction>,
   val seed: Int = 0,
) {
   @Transient val random: Random = Random(0)
   @Transient val functionMap = TreeMap<Float, IFSFunction>()
   init {
      var w = 0F
      for (f in functions) {
         w += f.weight
         functionMap[w] = f
      }
   }
   @Transient val totalWeight = functions.sumOf { it.weight.D }.F

   fun randomFunction() =
      functionMap.ceilingEntry(random.nextFloat() * totalWeight)?.value ?: functionMap.lastValue()

   /**
    * Render the IFS into an asynchronous flow of points.
    *
    * Specify a negative [samples] amount to obtain an infinite flow.
    */
   fun render(samples: Int, iterationsPerSample: Int, traceFrom: Int = 10): Flow<PointObject2D> {
      if (functions.isEmpty()) return emptyFlow()
      return channelFlow {
         coroutineScope {
            var i = 0
            while (i < samples) launch(Dispatchers.Default) {
               var p = Vec2f(random.nextFloat() * 2F - 1F, random.nextFloat() * 2F - 1F)
               lateinit var c: Color
               for (j in 0 until iterationsPerSample) {
                  val f = randomFunction()
                  p = f.transform * p
                  c = if (j == 0) f.color else c.mix(f.color, 0.5F)
                  if (j >= traceFrom) send(PointObject2D(p, PointStyle(c, 1F)))
               }
            }.also { i++ }
         }
      }
   }

   override fun toString() = "IFS(${functions.joinToString(", ")}, seed=$seed)"

   companion object {
      private val vec2fPattern = Regex(
         """\s*+\{\s*+(?<x>[+-]?(?:\d*+\.\d++|\d++\.?))\s*+,\s*+(?<y>[+-]?(?:\d*+\.\d++|\d++\.?))\s*+}\s*+""")
      private val matPattern = Regex(
         """\s*+\{\s*+(?<a>\{[\s\S]*?})\s*+,\s*+(?<b>\{[\s\S]*?})\s*+}\s*+""")
      private val transformPattern = Regex(
         """\s*+List\s*+\[(?<mat>\{(?:\s*+,?\s*+\{[\s\S]*?})+\s*+})\s*+,\s*+(?<t>\{[\s\S]*?})\s*+]""")
      private val functionList = Regex(
         """\s*+List\s*+\[(?<functions>[\s\S]*)]\s*+;?\s*+""")
      fun importFromText(text: String): IFS? {
         val m = functionList.matchEntire(text) ?: return null
         val functions = m.groups["functions"]!!.value
         val transforms = transformPattern.findAll(functions).map {
            fun MatchGroup?.NN() = this?.value ?: throw IllegalArgumentException("Unexpected format: $text")
            fun MatchResult?.NN() = this ?: throw IllegalArgumentException("Unexpected format: $text")
            fun MatchGroup?.F() = this.NN().toFloatOrNull() ?: throw IllegalArgumentException("Unexpected format: $text")

            val mat = matPattern.matchEntire(it.groups["mat"].NN()).NN()
            val a = vec2fPattern.matchEntire(mat.groups["a"].NN()).NN()
            val b = vec2fPattern.matchEntire(mat.groups["b"].NN()).NN()
            val t = vec2fPattern.matchEntire(it.groups["t"].NN()).NN()

            Transform2D(a.groups["x"].F(), a.groups["y"].F(), b.groups["x"].F(), b.groups["y"].F(), t.groups["x"].F(), t.groups["y"].F())
         }
         try {
            return IFS(transforms.map {
               IFSFunction(it, 1F, Color.randomHue())
            }.toList())
         } catch (e: IllegalArgumentException) {
            println(e)
            return null
         }
      }

      fun exportToText(ifs: IFS) = "List[\n  ${
         ifs.functions.joinToString(",\n  ") {
            val t = it.transform
            fun Float.R() = roundToString(4, true).removeTrailingZeros() 
            "List[{{${t.a.R()}, ${t.b.R()}}, {${t.c.R()}, ${t.d.R()}}}, {${t.e.R()}, ${t.f.R()}}]"
         }
      }\n];"
   }
}