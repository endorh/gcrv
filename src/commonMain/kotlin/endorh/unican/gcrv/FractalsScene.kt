package endorh.unican.gcrv

import de.fabmax.kool.KoolContext
import de.fabmax.kool.demo.SimpleScene
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.FullscreenShaderUtil
import de.fabmax.kool.pipeline.FullscreenShaderUtil.generateFullscreenQuad
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addMesh

class FractalsScene : SimpleScene("Fractals") {
   override fun Scene.setupMainScene(ctx: KoolContext) {
      setupUiScene(true)
      addColorMesh {
         generate {
            rect {
               size.set(720F, 480F)
               // origin.set(-360F, -240F)
            }
         }
      }
      addMesh(Attribute.POSITIONS) {
         generateFullscreenQuad()
         shader = MandelbrotShader()
      }
   }
}

class MandelbrotShader : KslShader(Model(), pipelineConfig) {
   private class Model : KslProgram("Mandelbrot shader") {
      init {
         // val screenPos = interStageFloat2()

         // vertexStage {
         //    main {
         //       val vertexPos = float4Var(float4Value(vertexAttribFloat3(Attribute.POSITIONS.name), 1F))
         //       screenPos.input set vertexPos.xy
         //       outPosition set mvpMatrix().matrix * vertexPos
         //    }
         // }
         fragmentStage {
            main {
               val color = float4Var()
               color.r set inFragPosition.x
               color.g set inFragPosition.y
               color.b set inFragPosition.x / 720F.const
               // colorOutput(color.rgb * color.a, color.a)
               colorOutput(float4Value(1F, 0F, 0F, 1F))
            }
         }
      }
   }

   companion object {
      private val pipelineConfig = FullscreenShaderUtil.fullscreenShaderPipelineCfg
   }
}