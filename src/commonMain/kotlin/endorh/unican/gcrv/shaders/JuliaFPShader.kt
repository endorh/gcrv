package endorh.unican.gcrv.shaders

import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.Ui2Shader
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.uint1VarAssignInt1

class JuliaFPShader : KslShader(Model(), pipelineConfig) {
   var iterations by uniform1i("uIterations", 1)
   var c by uniform2f("uC", Vec2f(0F, 0F))

   private class Model : KslProgram("Mandelbrot simple precission floating point shader") {
      init {
         val iterations = uniformInt1("uIterations")
         val c = uniformFloat2("uC")
         // val gradient = uniformFloat1Array("gradient", 64)
         val texCoords = interStageFloat2()
         val screenPos = interStageFloat2()
         val tint = interStageFloat4()
         val clipBounds = interStageFloat4(interpolation = KslInterStageInterpolation.Flat)

         vertexStage {
            main {
               texCoords.input set vertexAttribFloat2(Attribute.TEXTURE_COORDS.name)
               tint.input set vertexAttribFloat4(Attribute.COLORS.name)
               clipBounds.input set vertexAttribFloat4(Ui2Shader.ATTRIB_CLIP.name)

               val vertexPos = float4Var(float4Value(vertexAttribFloat3(Attribute.POSITIONS.name), 1f))
               screenPos.input set vertexPos.xy
               outPosition set mvpMatrix().matrix * vertexPos
            }
         }

         fragmentStage {
            main {
               `if`(
                  all(screenPos.output gt clipBounds.output.xy) and
                    all(screenPos.output lt clipBounds.output.zw)
               ) {
                  val i = uint1Var(0u.const)
                  val itersI = int1Var(iterations)
                  val iters = uint1Var()
                  uint1VarAssignInt1(iters, itersI)
                  val cr = float1Var(c.x)
                  val cc = float1Var(c.y)
                  val zr = float1Var(texCoords.output.x)
                  val zc = float1Var(texCoords.output.y)
                  val nz = float1Var()
                  val nc = float1Var()
                  val normSquared = float1Var(zr * zr + zc * zc)
                  val maxNormSquared = float1Var(normSquared)
                  `while`((i lt iters) and (normSquared lt 4F.const)) {
                     nz set zr * zr - zc * zc + cr
                     nc set zr * zc * 2F.const + cc
                     zr set nz
                     zc set nc
                     normSquared set zr * zr + zc * zc
                     maxNormSquared set max(maxNormSquared, normSquared)
                     i set i + 1u.const
                  }
                  val color = float4Var()
                  color.a set 1F.const
                  `if`(i ge iters) {
                     color.r set 0F.const
                     color.g set maxNormSquared / 8F.const
                     color.b set 0F.const
                  }.`else` {
                     val quotient = float1Var(i.F / iters.F)
                     quotient set (4F.const + log2(quotient)) / 4F.const
                     val rr = quotient
                     val bb = 1F.const - quotient
                     color.r set rr
                     color.g set 2F.const * rr * bb
                     color.b set bb
                  }
                  colorOutput(color.rgb * color.a, color.a)
               }.`else` {
                  discard()
               }
            }
         }
      }
   }

   companion object {
      private val pipelineConfig = PipelineConfig().apply {
         blendMode = BlendMode.BLEND_PREMULTIPLIED_ALPHA
         cullMethod = CullMethod.NO_CULLING
         depthTest = DepthCompareOp.DISABLED
      }
   }
}