package endorh.unican.gcrv.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.scene.geometry.RectProps
import de.fabmax.kool.scene.geometry.Usage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface ShaderImageScope : UiScope {
   override val modifier: ShaderImageModifier
}

open class ShaderImageModifier(surface: UiSurface) : UiModifier(surface) {
   var shader: Shader? by property(null)
   var imageSize: ImageSize by property(ImageSize.FixedScale(1F))
   var uvRect: UvRect by property(UvRect.FULL)
}

fun <T: ShaderImageModifier> T.shader(shader: Shader): T = apply { this.shader = shader }
fun <T: ShaderImageModifier> T.imageSize(imageSize: ImageSize): T = apply { this.imageSize = imageSize }
fun <T: ShaderImageModifier> T.uvRect(uvRect: UvRect): T = apply { this.uvRect = uvRect }

@OptIn(ExperimentalContracts::class)
inline fun UiScope.ShaderImage(
   shader: Shader? = null,
   scopeName: String? = null,
   block: ShaderImageScope.() -> Unit = {}
) : ShaderImageScope {
   contract {
      callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
   }

   val image = uiNode.createChild(scopeName, ShaderImageNode::class, ShaderImageNode.factory)
   shader?.let {
      image.modifier.shader(it)
   }
   image.block()
   return image
}

class ShaderImageNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), ShaderImageScope {
   override val modifier = ShaderImageModifier(surface)
   var mesh: ShaderImageMesh? = null

   private fun RectProps.applyUvRect(uvRect: UvRect) {
      texCoordUpperLeft.set(uvRect.topLeft)
      texCoordLowerLeft.set(uvRect.bottomLeft)
      texCoordUpperRight.set(uvRect.topRight)
      texCoordLowerRight.set(uvRect.bottomRight)
   }

   override fun render(ctx: KoolContext) {
      super.render(ctx)

      modifier.shader?.let { shader ->
         surface.getMeshLayer(modifier.zLayer).addCustomLayer("shaderImage") {
            ShaderImageMesh().also { mesh = it }.apply {
               builder.clear()
               builder.configured {
                  centeredRect {
                     applyUvRect(modifier.uvRect)

                     origin.set(widthPx / 2, heightPx / 2, 0F)
                     size.set(innerWidthPx, innerHeightPx)
                  }
               }
               applyShader(modifier.shader)
            } //.also { mesh = it }
         }
         mesh?.apply {
            builder.clear()
            builder.configured {
               centeredRect {
                  applyUvRect(modifier.uvRect)

                  origin.set(widthPx / 2, heightPx / 2, 0F)
                  size.set(innerWidthPx, innerHeightPx)
               }
            }
            applyShader(modifier.shader)
         }
      }
   }

   companion object {
      val factory: (UiNode?, UiSurface) -> ShaderImageNode = ::ShaderImageNode
   }
}

class ShaderImageMesh : Mesh(IndexedVertexList(Ui2Shader.UI_MESH_ATTRIBS)) {
   var defaultShader: Shader? = null
   val builder = MeshBuilder(geometry).apply { isInvertFaceOrientation = true }

   init {
      geometry.usage = Usage.DYNAMIC
      isCastingShadow = false
   }

   private fun getOrCreateDefaultShader() = defaultShader ?: Ui2Shader().also { defaultShader = it }

   fun applyShader(shader: Shader?) {
      this.shader = shader ?: getOrCreateDefaultShader()
   }
}