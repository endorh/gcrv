package endorh.unican.gcrv.l_system

import de.fabmax.kool.math.randomF
import de.fabmax.kool.math.toRad
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.scene.copy
import endorh.unican.gcrv.scene.objects.LineObject2D
import endorh.unican.gcrv.serialization.TreeMap
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.transformations.TaggedTransform2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.util.signedAngle
import endorh.unican.gcrv.util.times
import endorh.unican.gcrv.util.unaryMinus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.yield
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.atan2
import kotlin.random.Random

@Serializable(PolymorphicSerializer::class)
sealed interface LSystemRule {
   val weight: Float
   fun apply(state: String): Pair<String, String>?
}

@Serializable @SerialName("char")
class CharRule(val char: Char, val replacement: String, override val weight: Float = 1F) : LSystemRule {
   override fun apply(state: String): Pair<String, String>? {
      if (state.startsWith(char)) return state.substring(1) to replacement
      return null
   }
}

@Serializable @SerialName("regex")
class RegexRule(val pattern: String, val replacement: String, override val weight: Float = 1F) : LSystemRule {
   @Transient val regex = Regex(pattern)
   override fun apply(state: String): Pair<String, String>? {
      regex.matchAt(state, 0)?.let {
         if (it.range.start == 0) {
            return state.substring(it.range.endInclusive + 1) to replacement
         }
      }
      return null
   }
}

@Serializable
data class LSystem(
   val axiom: String,
   val rules: List<LSystemRule>,
) {
   suspend fun generate(steps: Int): String {
      var state = axiom
      for (i in 0 until steps) {
         yield()
         state = step(state)
      }
      return state
   }
   suspend fun step(state: String): String {
      var pending = state
      var result = ""
      while (pending.isNotEmpty()) {
         yield()
         val r = rules.mapNotNull { rule -> rule.apply(pending)?.let { rule to it } }
         if (r.isEmpty()) {
            result += pending[0]
            pending = pending.substring(1)
         } else {
            val tree = TreeMap<Float, Pair<String, String>>()
            var accum = 0F
            for (p in r) {
               accum += p.first.weight
               tree[accum] = p.second
            }
            val (p, a) = tree.ceilingValue(Random.randomF() * accum) ?: tree.lastValue()
            pending = p
            result += a
         }
      }
      return result
   }

   companion object {
      private val LINE_PATTERN = Regex(
         """^(?:(?<weight>\d+\.\d*|\.?\d+):\s*)?(?<pattern>\S+)\s*(?<replacement>.*)$""",
         RegexOption.MULTILINE)
      private val COMMENT_PATTERN = Regex("""^\s*#.*""", RegexOption.MULTILINE)

      fun import(text: String) {
         val axiom = text.lineSequence().firstOrNull() ?: ""
         val rules = text.lineSequence().drop(1).filter {
            it.isNotBlank() && !COMMENT_PATTERN.matches(it)
         }.mapNotNull { LINE_PATTERN.matchEntire(it) }
      }

      fun export(lSystem: LSystem) {

      }
   }
}

interface LSystemRenderingRuleScope {
   fun draw(geometry: Object2D)
   fun draw(vararg geometry: Object2D) = geometry.forEach { draw(it) }
   fun draw(geometry: Collection<Object2D>) = geometry.forEach { draw(it) }

   fun transform(transform: (LSystemState) -> LSystemState)
   fun transform(transform: Transform2D) = transform { it.transformLocally(transform) }
   fun transform(transform: TaggedTransform2D) = transform(transform.toTransform())

   fun push()
   fun pop()
}

class LSystemRenderingRuleScopeImpl(
   var state: LSystemState,
   val stack: MutableList<LSystemState> = mutableListOf(),
   val geometry: MutableList<Object2D> = mutableListOf()
) : LSystemRenderingRuleScope {
   fun applyTransform(t: Transform2D, o: Object2D) {
      for (prop in o.geometry)
         prop.applyTransform(t)
      for (child in o.children)
         applyTransform(t, child)
   }

   fun transform(geometry: Object2D) = geometry.apply {
      val transform = TaggedTransform2D(
         rotation = Vec2f.X_AXIS.signedAngle(state.direction),
         translate = state.position)
      applyTransform(transform.toTransform(), this)
   }
   override fun draw(geometry: Object2D) {
      this.geometry.add(transform(geometry))
   }

   override fun transform(transform: (LSystemState) -> LSystemState) {
      state = transform(state)
   }

   override fun push() {
      stack.add(state)
   }
   override fun pop() {
      state = stack.removeLast()
   }
}



@Serializable(PolymorphicSerializer::class)
fun interface LSystemRenderingRule {
   fun LSystemRenderingRuleScope.apply()
}

@Serializable @SerialName("draw")
data class DrawRenderingRule(val geometry: List<Object2D>) : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      draw(geometry.map { it.copy() })
   }
}
@Serializable @SerialName("move")
data class MoveRenderingRule(val offset: Float) : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      transform(Transform2D.translate(offset, 0F))
   }
}
@Serializable @SerialName("move-draw")
data class MoveDrawRenderingRule(val offset: Float) : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      draw(LineObject2D(Vec2f.ZERO, Vec2f.X_AXIS * offset))
      transform(Transform2D.translate(offset, 0F))
   }
}
@Serializable @SerialName("turn")
data class TurnRenderingRule(val rotationDeg: Float) : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      transform(Transform2D.rotate(rotationDeg.toRad()))
   }
}
@Serializable @SerialName("push")
data object PushRenderingRule : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      push()
   }
}
@Serializable @SerialName("pop")
data object PopRenderingRule : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      pop()
   }
}
@Serializable @SerialName("seq")
data class CompositeRenderingRule(val rules: List<LSystemRenderingRule>) : LSystemRenderingRule {
   override fun LSystemRenderingRuleScope.apply() {
      rules.forEach {
         with (it) {
            apply()
         }
      }
   }
}

@Serializable
data class LSystemRenderer(
   val rules: Map<Char, LSystemRenderingRule>
) {
   fun render(code: String, initialPosition: Vec2f, initialDirection: Vec2f): Flow<Object2D> {
      val scope = LSystemRenderingRuleScopeImpl(LSystemState(initialPosition, initialDirection, 0))
      val chars = code.asSequence()
      return channelFlow {
         coroutineScope {
            try {
               chars.forEach {
                  rules[it]?.apply { scope.apply() }
                  for (geo in scope.geometry) send(geo)
                  scope.geometry.clear()
               }
            } catch (e: NoSuchElementException) {
               println("LSystemRenderer ran out of stack!")
            }
         }
      }
   }

   companion object {
      val TurtleProgramRenderer = LSystemRenderer(mapOf(
         'F' to MoveDrawRenderingRule(1F),
         'A' to MoveDrawRenderingRule(1F),
         'B' to MoveDrawRenderingRule(1F),
         'C' to MoveDrawRenderingRule(1F),
         'G' to MoveDrawRenderingRule(1F),
         '/' to TurnRenderingRule(60F),
         '\\' to TurnRenderingRule(-60F),
         'l' to TurnRenderingRule(25F),
         'r' to TurnRenderingRule(-25F),
         '+' to TurnRenderingRule(90F),
         '-' to TurnRenderingRule(-90F),
         '[' to PushRenderingRule,
         ']' to PopRenderingRule
      ))
   }
}

@Serializable
data class LSystemState(
   val position: Vec2f,
   val direction: Vec2f,
   val depth: Int,
) {
   fun transformLocally(t: Transform2D): LSystemState {
      val d = t linearTransform direction
      val p = (
          Transform2D.translate(position)
        * Transform2D.scale(d.length())
        * Transform2D.rotate(atan2(d.y, d.x)) * t
        * Transform2D.translate(-position)
      ) * position
      return LSystemState(p, d, depth)
   }
}
