package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.serialization.Vec2i
import endorh.unican.gcrv.util.I

open class ResizableBufferCanvas(
   width: Int = 100, height: Int = 100, autoUpdate: Boolean = false, bufferNum: Int = 2
) : Canvas {
   val canvasState = mutableStateOf(
      BufferCanvas(width, height, autoUpdate, bufferNum))
   var canvas: BufferCanvas
      get() = canvasState.value
      protected set(value) {
         canvasState.value = value
      }
   open var autoUpdate
      get() = canvas.autoUpdate
      set(value) {
         canvas.autoUpdate = value
      }
   open var bufferNum = bufferNum
      set(value) {
         field = value
         resize()
      }
   protected open val onResize = mutableListOf<(ResizableBufferCanvas) -> Unit>()

   private var suppressResize = false
   fun resize(width: Int, height: Int) {
      suppressResize = true
      this.width = width
      suppressResize = false
      this.height = height
   }
   protected open fun resize() {
      canvas = makeCanvas()
      onResize.forEach { it(this) }
   }
   protected open fun makeCanvas() = BufferCanvas(width, height, autoUpdate, bufferNum)

   override var width = width
      set(value) {
         field = value
         if (!suppressResize) resize()
      }
   override var height = height
      set(value) {
         field = value
         if (!suppressResize) resize()
      }
   var size
      get() = Vec2i(width, height)
      set(value) {
         resize(value.x, value.y)
      }
   override val texture get() = canvas.texture

   open fun onResize(block: (ResizableBufferCanvas) -> Unit) = apply {
      onResize += block
   }
   open fun removeOnResize(block: (ResizableBufferCanvas) -> Unit) {
      onResize -= block
   }
}

fun UiScope.ResizableCanvas(
   canvas: ResizableBufferCanvas,
   block: CanvasScope.() -> Unit = {}
) {
   Canvas(canvas.canvasState.use()) {
      modifier.size(Grow.Std, Grow.Std).onPositioned {
         if (it.innerWidthPx.I != canvas.width || it.innerHeightPx.I != canvas.height)
            canvas.resize(it.innerWidthPx.I, it.innerHeightPx.I)
      }.uvRect(UvRect.FULL).canvasSize(CanvasSize.FixedScale(1F))
      block()
   }
}