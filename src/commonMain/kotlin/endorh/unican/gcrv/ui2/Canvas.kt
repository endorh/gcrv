package endorh.unican.gcrv.ui2

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min

interface CanvasScope : UiScope {
   override val modifier: CanvasModifier

   fun Vec2f.toCanvasCoordinates(): MutableVec2f = MutableVec2f(this).convertToCanvasCoordinates()
   fun MutableVec2f.convertToCanvasCoordinates(): MutableVec2f = this
   val PointerEvent.canvasPosition: MutableVec2f get() = position.convertToCanvasCoordinates()
}

interface Canvas {
   /**
    * The width of the canvas in pixels.
    */
   val width: Int
   /**
    * The height of the canvas in pixels.
    */
   val height: Int
   /**
    * The aspect ratio of the canvas, i.e. width / height.
    */
   val aspectRatio: Float get() = width.toFloat() / height.toFloat()

   /**
    * The texture displaying the canvas contents.
    */
   val texture: Texture2d

   fun Vec2f.toCanvasCoordinates(): Vec2f = MutableVec2f(this).convertToCanvasCoordinates()
   fun MutableVec2f.convertToCanvasCoordinates(): MutableVec2f = this
}

data class UvRect(
   val topLeft: Vec2f = Vec2f.ZERO,
   val topRight: Vec2f = Vec2f.X_AXIS,
   val bottomLeft: Vec2f = Vec2f.Y_AXIS,
   val bottomRight: Vec2f = Vec2f.ONE
) {
   companion object {
      private val Vec2f.Companion.ONE: Vec2f get() = Vec2f(1F, 1F)
      val FULL = UvRect()
      val Y_FLIP = UvRect(
         topLeft = Vec2f.Y_AXIS,
         topRight = Vec2f.ONE,
         bottomLeft = Vec2f.ZERO,
         bottomRight = Vec2f.X_AXIS
      )
      val X_FLIP = UvRect(
         topLeft = Vec2f.X_AXIS,
         topRight = Vec2f.ZERO,
         bottomLeft = Vec2f.ONE,
         bottomRight = Vec2f.Y_AXIS
      )
      val FLIP = UvRect(
         topLeft = Vec2f.ONE,
         topRight = Vec2f.Y_AXIS,
         bottomLeft = Vec2f.ZERO,
         bottomRight = Vec2f.X_AXIS
      )
   }
}


open class CanvasModifier(surface: UiSurface) : UiModifier(surface) {
   var canvas: Canvas? by property(null)
   var tint: Color by property(Color.WHITE)
   var customShader: Shader? by property(null)
   var canvasZ: Int by property(0)
   var canvasSize: CanvasSize by property(CanvasSize.FitContent)
   var invertY: Boolean by property(false)
   var uvRect: UvRect? by property(UvRect.Y_FLIP)
}

fun <T : CanvasModifier> T.canvas(canvas: Canvas?): T = apply {
   this.canvas = canvas
}

fun <T : CanvasModifier> T.tint(color: Color): T = apply {
   tint = color
}

fun <T : CanvasModifier> T.canvasZ(canvasZ: Int): T = apply {
   this.canvasZ = canvasZ
}

fun <T : CanvasModifier> T.canvasSize(size: CanvasSize): T = apply {
   canvasSize = size
}

fun <T : CanvasModifier> T.customShader(shader: Shader): T = apply {
   customShader = shader
}

fun <T : CanvasModifier> T.uvRect(uvRect: UvRect?): T = apply {
   this.uvRect = uvRect
}

fun <T : CanvasModifier> T.invertY(y: Boolean): T = apply {
   invertY = y
}

sealed class CanvasSize {
   data object Stretch : CanvasSize()
   data object ZoomContent : CanvasSize()
   data object FitContent : CanvasSize()
   data class FixedScale(val scale: Float = 1F) : CanvasSize()
}

@OptIn(ExperimentalContracts::class)
inline fun UiScope.Canvas(
   canvas: Canvas? = null,
   scopeName: String? = null,
   block: CanvasScope.() -> Unit
): CanvasScope {
   contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
   }

   return uiNode.createChild(scopeName, CanvasNode::class, CanvasNode.factory).apply {
      modifier.canvas(canvas)
      block()
   }
}

open class CanvasNode(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), CanvasScope {
   override val modifier = CanvasModifier(surface)

   override fun MutableVec2f.convertToCanvasCoordinates(): MutableVec2f {
      val cw = canvasWidth.value
      val ch = canvasHeight.value
      val aw = widthPx
      val ah = heightPx
      if (modifier.invertY) y = ah - y
      when (val size = modifier.canvasSize) {
         CanvasSize.FitContent -> {
            if (aw > cw) x -= (aw - cw) / 2F
            else y -= (ah - ch) / 2F
         }
         CanvasSize.Stretch -> {
            x *= cw / aw
            y *= ch / ah
         }
         CanvasSize.ZoomContent -> {
            val s = min(cw / aw, ch / ah)
            x = (x - aw / 2F) * s + cw / 2F
            y = (y - ah / 2F) * s + ch / 2F
         }
         is CanvasSize.FixedScale -> {
            x /= size.scale
            y /= size.scale
         }
      }
      modifier.canvas?.run {
         convertToCanvasCoordinates()
      }
      return this
   }

   val canvasWidth = mutableSerialStateOf(1)
   val canvasHeight = mutableSerialStateOf(1)

   private var canvasAR = 1F

   override fun measureContentSize(ctx: KoolContext) {
      canvasAR = 1F
      if (modifier.canvasSize != CanvasSize.Stretch) {
         canvasAR = modifier.canvas?.aspectRatio ?: 1F
         updateImageSize()
      }

      val modWidth = modifier.width
      val modHeight = modifier.height
      val measuredWidth: Float
      val measuredHeight: Float

      val padH = paddingStartPx + paddingEndPx
      val padV = paddingTopPx + paddingBottomPx

      when {
         modWidth is Dp && modHeight is Dp -> {
            measuredWidth = modWidth.px
            measuredHeight = modHeight.px
         }

         modWidth is Dp && modHeight !is Dp -> {
            // fixed width, measured height depends on width and chosen image size mode
            measuredWidth = modWidth.px
            measuredHeight = computeHeight(measuredWidth - padH, canvasAR) + padV
         }

         modWidth !is Dp && modHeight is Dp -> {
            // fixed height, measured width depends on height and chosen image size mode
            measuredHeight = modHeight.px
            measuredWidth = computeWidth(measuredHeight - padV, canvasAR) + padH
         }

         else -> {
            // dynamic (fit / grow) width and height
            val scale = (modifier.canvasSize as? CanvasSize.FixedScale)?.scale ?: 1F
            measuredWidth = canvasWidth.use() * scale + padH
            measuredHeight = canvasHeight.use() * scale + padV
         }
      }
      setContentSize(measuredWidth, measuredHeight)
   }

   private fun updateImageSize() {
      canvasWidth.set(max(1, modifier.canvas?.width ?: 1))
      canvasHeight.set(max(1, modifier.canvas?.height ?: 1))
   }

   private fun computeWidth(measuredHeightPx: Float, canvasAR: Float): Float {
      // make sure we use() image dimensions, so we get updated when they change
      canvasWidth.use()
      val imgH = canvasHeight.use()
      return when (val sz = modifier.canvasSize) {
         CanvasSize.Stretch -> measuredHeightPx
         CanvasSize.FitContent -> measuredHeightPx * canvasAR
         CanvasSize.ZoomContent -> measuredHeightPx * canvasAR
         is CanvasSize.FixedScale -> imgH * sz.scale
      }
   }

   private fun computeHeight(measuredWidthPx: Float, canvasAR: Float): Float {
      // make sure we use() image dimensions, so we get updated when they change
      canvasHeight.use()
      val imgW = canvasWidth.use()
      return when (val sz = modifier.canvasSize) {
         CanvasSize.Stretch -> measuredWidthPx
         CanvasSize.FitContent -> measuredWidthPx / canvasAR
         CanvasSize.ZoomContent -> measuredWidthPx / canvasAR
         is CanvasSize.FixedScale -> imgW * sz.scale
      }
   }

   override fun render(ctx: KoolContext) {
      super.render(ctx)
      modifier.canvas?.texture?.let {
         val imgMesh = surface.getMeshLayer(modifier.zLayer + modifier.canvasZ).addImage(it)
         imgMesh.builder.clear()
         imgMesh.builder.configured(modifier.tint) {
            centeredRect {
               isCenteredOrigin = false

               modifier.uvRect?.let { rect ->
                  texCoordLowerLeft.set(rect.bottomLeft)
                  texCoordLowerRight.set(rect.bottomRight)
                  texCoordUpperLeft.set(rect.topLeft)
                  texCoordUpperRight.set(rect.topRight)
               }

               val imgW = canvasWidth.value
               val imgH = canvasHeight.value
               val cx = widthPx * 0.5F
               val cy = heightPx * 0.5F

               when (val sz = modifier.canvasSize) {
                  CanvasSize.FitContent -> {
                     val s = min(innerWidthPx / imgW, innerHeightPx / imgH)
                     origin.set(cx - imgW * s * 0.5F, cy - imgH * s * 0.5F, 0F)
                     size.set(imgW * s, imgH * s)
                  }

                  CanvasSize.ZoomContent -> {
                     val s = max(innerWidthPx / imgW, innerHeightPx / imgH)
                     origin.set(cx - imgW * s * 0.5F, cy - imgH * s * 0.5F, 0F)
                     size.set(imgW * s, imgH * s)
                  }

                  CanvasSize.Stretch -> {
                     origin.set(paddingStartPx, paddingTopPx, 0F)
                     size.set(innerWidthPx, innerHeightPx)
                  }

                  is CanvasSize.FixedScale -> {
                     val s = sz.scale
                     origin.set(cx - canvasWidth.value * s * 0.5F, cy - canvasHeight.value * s * 0.5F, 0F)
                     size.set(canvasWidth.value * s, canvasHeight.value * s)
                  }
               }
            }
         }
         imgMesh.applyShader(it, modifier.customShader)
      }
   }

   companion object {
      val factory: (UiNode, UiSurface) -> CanvasNode = { parent, surface -> CanvasNode(parent, surface) }
   }
}
