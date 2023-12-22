package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.math.MutableVec2d
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec4i
import de.fabmax.kool.math.clamp
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.util.Color
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.serialization.Vec2f
import endorh.unican.gcrv.shaders.MandelbrotFPShader
import endorh.unican.gcrv.ui2.ShaderImage
import endorh.unican.gcrv.ui2.UvRect
import endorh.unican.gcrv.ui2.shader
import endorh.unican.gcrv.ui2.uvRect
import endorh.unican.gcrv.util.*
import endorh.unican.gcrv.windows.BaseWindow
import kotlin.math.log2
import kotlin.math.pow

class MandelbrotWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("Mandelbrot Fractal", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val rect = Rect2d(-2.0, -1.0, 1.0, 1.0)
   val coordsRect get() = Rect2d(0.0, 0.0, imageWidth.D, imageHeight.D)
   val displayedRect: Rect2d get() {
      val w: Double
      val h: Double
      val zoom = zoomLevel.value
      val center = zoomCenter.value
      val rectAR = rect.height / rect.width
      val imageAR = imageHeight.D / imageWidth.D
      if (imageAR > rectAR) {
         w = rect.width / zoom
         h = rect.height / zoom * imageAR / rectAR
      } else {
         w = rect.width / zoom * rectAR / imageAR
         h = rect.height / zoom
      }

      val sx = center.x - w / 2
      val sy = center.y - h / 2
      return Rect2d(sx, sy, sx + w, sy + h)
   }
   val uvRect get() = displayedRect.let {
      UvRect(
         Vec2f(it.left.F, it.top.F),
         Vec2f(it.right.F, it.top.F),
         Vec2f(it.left.F, it.bottom.F),
         Vec2f(it.right.F, it.bottom.F))
   }
   val zoomLevel = mutableStateOf(1.0)
   val zoomCenter = mutableStateOf(rect.center)
   val zoomAnchor = mutableStateOf(rect.center)
   var imageWidth = 1F
   var imageHeight = 1F

   var dragPanning = false

   val mandelbrotIterations = mutableStateOf(10000UL).onChange {
      mandelbrotShader.iterations = it.toInt()
   }

   val mandelbrotShader: MandelbrotFPShader = MandelbrotFPShader().apply {
      iterations = mandelbrotIterations.value.toInt()
   }

   fun zoomBy(delta: Double) {
      val nextZoom = (zoomLevel.value * 2.0.pow(delta)).clamp(1.0, 1e8)
      val factor = zoomLevel.value / nextZoom
      val anchor = zoomAnchor.value
      val displayedRect = displayedRect
      val center = MutableVec2d(displayedRect.scaleFrom(anchor, factor).center)
      val w = displayedRect.width * factor
      val h = displayedRect.height * factor
      if (center.x + w / 2 > rect.right) center.x = rect.right - w / 2
      if (center.x - w / 2 < rect.left) center.x = rect.left + w / 2
      if (center.y + h / 2 > rect.bottom) center.y = rect.bottom - h / 2
      if (center.y - h / 2 < rect.top) center.y = rect.top + h / 2
      zoomCenter.value = Vec2d(center)
      zoomLevel.value = nextZoom
   }

   fun setAnchorByCoords(coords: Vec2d) {
      zoomAnchor.value = displayedRect.globalize(coordsRect.relativize(coords))
   }

   override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
      modifier.padding(horizontal = sizes.smallGap, vertical = sizes.smallGap)

      Row {
         modifier.margin(sizes.smallGap)
         Slider(zoomLevel.use().F, 1F, 20F) {
            modifier.onChange {
               zoomLevel.value = it.toDouble()
            }
         }
         Text("Zoom: ${zoomLevel.use().F.roundToString(3)}") {
            modifier.margin(start = sizes.smallGap)
         }

         Slider(log2(mandelbrotIterations.use().F), 1F, 15F) {
            modifier.onChange {
               mandelbrotIterations.value = 2UL pow it.UI
            }
         }
         Text("Iterations: ${mandelbrotIterations.use()}") {
            modifier.margin(start = sizes.smallGap)
         }
      }

      Box {
         modifier.size(Grow.Std, Grow.Std)
            .onMeasured {
               imageWidth = maxOf(1F, it.widthPx)
               imageHeight = maxOf(1F, it.heightPx)
            }.onWheelY {
               val pos = it.position
               setAnchorByCoords(Vec2d(pos.x.D, imageHeight.D - pos.y.D))
               zoomBy(it.pointer.deltaScrollY)
            }.onDragStart {
               dragPanning = true
            }.onDrag {
               if (dragPanning) {
                  val x = it.pointer.deltaX
                  val y = it.pointer.deltaY
                  val displayed = displayedRect
                  val dx = x / imageWidth * displayed.width
                  val dy = y / imageHeight * displayed.height
                  zoomCenter.value = Vec2d(zoomCenter.value.x - dx, zoomCenter.value.y + dy)
               }
            }.onDragEnd {
               dragPanning = false
            }
         ShaderImage {
            val zoomLevel = zoomLevel.use()
            val c = zoomCenter.use().toMutableVec2f()
            val w: Float
            val h: Float
            val rectAR = rect.height / rect.width
            val imageAR = imageHeight / imageWidth
            if (imageAR > rectAR) {
               w = (rect.width / zoomLevel).F
               h = (rect.height / zoomLevel * imageAR / rectAR).F
            } else {
               w = (rect.width / zoomLevel * rectAR / imageAR).F
               h = (rect.height / zoomLevel).F
            }

            val sx = c.x - w / 2
            val sy = c.y - h / 2
            val uvRect = UvRect(
               Vec2f(sx, sy),
               Vec2f(sx + w, sy),
               Vec2f(sx, sy + h),
               Vec2f(sx + w, sy + h))
            modifier
               .shader(mandelbrotShader)
               .size(Grow.Std, Grow.Std)
               .backgroundColor(Color.LIGHT_GREEN)
               .uvRect(uvRect)
         }
      }
   }
}