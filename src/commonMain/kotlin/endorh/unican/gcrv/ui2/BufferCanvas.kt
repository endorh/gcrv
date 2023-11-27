package endorh.unican.gcrv.ui2

import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.math.MutableVec2i
import de.fabmax.kool.pipeline.BufferedTexture2d
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.TextureData2d
import de.fabmax.kool.pipeline.TextureProps
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Uint8Buffer
import de.fabmax.kool.util.createUint8Buffer
import endorh.unican.gcrv.util.*
import kotlin.jvm.JvmInline

/**
 * [Canvas] backed by a [BufferedTexture2d] using [Uint8Buffer] with row-major [TexFormat.RGBA] format.
 *
 * For changes to the buffer to be reflected in the rendered canvas texture, [update] must be called.
 * Alternatively, [autoUpdate] can be set to true to automatically update the texture on all write
 * operations exposed by the class. Subclasses are responsible for preserving this contract.
 *
 * Updating the texture is an asynchronous operation, since it requires uploading the buffer contents
 * to the GPU. Calling the [update] method will only invalidate the current texture contents and
 * schedule an on-demand load of the new texture data, so it's a cheap operation.
 *
 * Rather than accessing the [buffer] directly, it's recommended to instead use the [get] and [set]
 * operators, or the [U] and [C] accessors, which provide [get] and [set] operations for [UInt] and
 * [Color] values, rather than [Int] values.
 *
 * ```kotlin
 * val canvas = BufferCanvas(256, 256)
 * canvas.update {                    // Batch an update operation
 *    canvas[0, 0] = 0x00FF00FF       // set pixel (0, 0) to green
 *    canvas.U[0, 1] = 0xFF0000FFu    // set pixel (0, 1) to red
 *    canvas.C[0, 2] = Color.BLUE     // set pixel (0, 2) to blue
 * }
 * // Alternatively, you may call `update` manually
 * canvas[0, 3] = 0x000000FF     // set pixel (0, 3) to black
 * canvas.update()
 * // Or set `autoUpdate` to `true`
 * canvas.autoUpdate = true
 * canvas.U[0, 4] = 0xFFFFFFFFu  // set pixel (0, 4) to gray and update
 * ```
 *
 * Fenced canvas access is provided by the [F] accessor, which also provides [UInt] and [Color]
 * sub-accessors.
 *
 * ```kotlin
 * canvas.F[256, 0]                 // returns `0` since `256` is not in range
 * canvas.F.U[0, -1] = 0xFFFFFFFFu  // does nothing since `-1` is not in range
 * ```
 *
 * For bulk operations use [toByteArray], [loadByteArray], [loadByteArraySlice] or
 * the [Int]/[UInt] variants.
 */
open class BufferCanvas(
   final override val width: Int,
   final override val height: Int,
   /**
    * Automatically schedule texture updates on every write operation, avoiding the need to
    * manually call [update] or batch operations within [update] blocks.
    */
   var autoUpdate: Boolean = false
) : Canvas {
   protected val texFormat = TexFormat.RGBA
   protected val pixelCount = width * height
   protected val bufferSize = width * height * texFormat.channels
   private var withinBatchUpdate: Boolean = false

   protected val buffer: Uint8Buffer = createUint8Buffer(bufferSize)
   protected var textureData = TextureData2d(buffer, width, height, texFormat)
      set(value) {
         field = value
         texture.updateTextureData(value)
      }
   override val texture = BufferedTexture2d(textureData, TextureProps(texFormat), "CanvasTexture")

   override fun MutableVec2f.convertToCanvasCoordinates() = apply {
      x += origin.x
      y += origin.y
   }

   /**
    * Canvas origin (top left corner) position.
    * Affects canvas accessors.
    */
   val origin = MutableVec2i()

   /**
    * Update the texture to reflect the current buffer contents.
    *
    * This is an asynchronous operation, since it requires uploading the buffer contents to the GPU.
    * Calling the [update] method will only invalidate the current texture contents and schedule an
    * on-demand load of the new texture data, so it's a cheap operation.
    *
    * To idiomatically run a batch of update operations, you may pass a lambda to the [update]
    * function, which will be run with [autoUpdate] switched off before a single [update] call.
    */
   fun update() {
      buffer.clear()
      texture.updateTextureData(textureData)
   }

   /**
    * Perform a batch of update operations, and then call [update] to update the texture.
    *
    * Batch updates are re-entrant, meaning if they happen within another batch update, only the
    * outer update will perform a texture update.
    */
   fun update(changes: BufferCanvas.() -> Unit) = apply {
      if (withinBatchUpdate) return@apply
      withinBatchUpdate = true
      val prev = autoUpdate
      autoUpdate = false
      changes()
      autoUpdate = prev
      withinBatchUpdate = false
      update()
   }

   protected fun bufferCoordinates(x: Int, y: Int): Int {
      val cx = x - origin.x
      val cy = y - origin.y
      checkIndices(cx, cy)
      return bufferIndex(cx, cy)
   }

   open fun bufferIndex(x: Int, y: Int) = y * width + x

   // Checks
   protected inline fun validIndex(i: Int) = 0 <= i && i < pixelCount
   protected inline fun validIndex(x: Int, y: Int) = 0 <= x && x < width && 0 <= y && y < height

   protected inline fun checkIndex(i: Int) {
      if (!validIndex(i))
         throw IndexOutOfBoundsException("Canvas buffer index out of bounds: $i for size $bufferSize")
   }
   protected inline fun checkIndices(x: Int, y: Int) {
      if (!validIndex(x, y))
         throw IndexOutOfBoundsException("Canvas indices out of bounds: ($x, $y) for size ($width, $height)")
   }

   // Accessors
   operator fun get(index: Int): Int {
      checkIndex(index)
      val base = index * 4
      return (buffer[base].toInt() and 0xFF shl 24) or (buffer[base + 1].toInt() and 0xFF shl 16) or
         (buffer[base + 2].toInt() and 0xFF shl 8) or (buffer[base + 3].toInt() and 0xFF)
   }
   operator fun set(index: Int, value: Int) {
      checkIndex(index)
      val base = index * 4
      buffer[base] = (value ushr 24).toByte()
      buffer[base + 1] = (value ushr 16).toByte()
      buffer[base + 2] = (value ushr 8).toByte()
      buffer[base + 3] = value.toByte()

      if (autoUpdate) update()
   }

   operator fun get(x: Int, y: Int) = bufferCoordinates(x, y).let { this[it] }
   operator fun set(x: Int, y: Int, value: Int) {
      val i = bufferCoordinates(x, y)
      this[i] = value
   }

   // Bulk operations
   /**
    * Clear the canvas, i.e., fill it with transparent black color.
    */
   fun clear(update: Boolean = autoUpdate) {
      for (i in 0 until bufferSize) buffer[i] = 0
      if (update) update()
   }

   /**
    * Fill the canvas with the given [color].
    */
   fun fill(color: Int, update: Boolean = autoUpdate) {
      for (i in 0 until pixelCount) this[i] = color
      if (update) update()
   }
   /**
    * Fill the canvas with the given [color].
    */
   fun fill(color: UInt, update: Boolean = autoUpdate) = fill(color.I, update)
   /**
    * Fill the canvas with the given [color].
    */
   fun fill(color: Color, update: Boolean = autoUpdate) = fill(color.RGBA.U.I, update)

   // Export/import operations
   /**
    * Export buffer contents to a row-major RGBA [ByteArray] of size [bufferSize], i.e.,
    * `width * height * 4`.
    */
   fun toByteArray() = buffer.toArray()

   /**
    * Export buffer contents to a row-major [IntArray] of size [pixelCount], i.e., `width * height`.
    */
   fun toIntArray() {
      val dest = IntArray(pixelCount)
      for (i in 0 until pixelCount) dest[i] = this[i]
   }

   /**
    * Export buffer contents to a row-major [UIntArray] of size [pixelCount], i.e., `width * height`.
    */
   @OptIn(ExperimentalUnsignedTypes::class)
   fun toUIntArray() {
      val dest = UIntArray(pixelCount)
      for (i in 0 until pixelCount) dest[i] = U[i]
   }

   private inline fun withBufferPos(position: Int, update: Boolean = false, body: () -> Unit) {
      buffer.position = position
      body()
      buffer.clear()
      if (update) update()
   }

   /**
    * Import buffer contents at the given [position] from a row-major RGBA [ByteArray].
    *
    * The array is expected to not have a length greater than `bufferSize - position`, where
    * [bufferSize] is `width * height * 4`.
    */
   fun loadByteArray(array: ByteArray, position: Int = 0, update: Boolean = autoUpdate) =
      withBufferPos(position, update) { buffer.put(array) }

   /**
    * Import buffer contents at the given [position] from a slice of a row-major RGBA [ByteArray]
    * determined by [offset] and [length].
    *
    * The imported [length] is expected to not be greater than `bufferSize - position`, where
    * [bufferSize] is `width * height * 4`.
    */
   fun loadByteArraySlice(array: ByteArray, position: Int = 0, offset: Int, length: Int, update: Boolean = autoUpdate) {
      withBufferPos(position, update) { buffer.put(array, offset, length) }
   }

   /**
    * Import [Int] buffer contents at the given [position] from a row-major [IntArray].
    *
    * The array is expected to not have a length greater than `pixelCount - position`, where
    */
   fun loadIntArray(array: IntArray, position: Int = 0, update: Boolean = autoUpdate) {
      withBufferPos(position * 4, update) { buffer.put(array.toByteArray()) }
   }

   /**
    * Import [Int] buffer contents at the given [position] from a slice of a row-major [IntArray]
    * determined by [offset] and [length].
    *
    * The imported [length] is expected to not be greater than `pixelCount - position`, where
    * [pixelCount] is `width * height`.
    */
   fun loadIntArraySlice(array: IntArray, position: Int = 0, offset: Int, length: Int, update: Boolean = autoUpdate) {
      withBufferPos(position * 4, update) {
         buffer.put(array.toByteArray(), offset * 4, length * 4)
      }
   }

   /**
    * Import [UInt] buffer contents at the given [position] from a row-major [UIntArray].
    *
    * The array is expected to not have a length greater than `pixelCount - position`, where
    * [pixelCount] is `width * height`.
    */
   @OptIn(ExperimentalUnsignedTypes::class)
   fun loadUIntArray(array: UIntArray, position: Int = 0, update: Boolean = autoUpdate) {
      loadIntArray(array.toIntArray(), position, update)
   }

   /**
    * Import [UInt] buffer contents at the given [position] from a slice of a row-major [UIntArray]
    * determined by [offset] and [length].
    *
    * The imported [length] is expected to not be greater than `pixelCount - position`, where
    * [pixelCount] is `width * height`.
    */
   @OptIn(ExperimentalUnsignedTypes::class)
   fun loadUIntArraySlice(array: UIntArray, position: Int = 0, offset: Int, length: Int, update: Boolean = autoUpdate) {
      loadIntArraySlice(array.toIntArray(), position, offset, length, update)
   }

   // Typed inline accessors
   /**
    * Provides [UInt] access to the canvas buffer.
    */
   inline val U: UnsignedAccessor get() = UnsignedAccessor(this)
   @JvmInline value class UnsignedAccessor(private val canvas: BufferCanvas) {
      operator fun get(index: Int): UInt = canvas[index].U
      operator fun set(index: Int, value: UInt) = canvas.set(index, value.I)

      operator fun get(index: UInt): UInt = this[index.I]
      operator fun set(index: UInt, value: UInt) = set(index.I, value)

      operator fun get(x: Int, y: Int) = canvas[x, y].U
      operator fun set(x: Int, y: Int, value: UInt) = canvas.set(x, y, value.I)

      operator fun get(x: UInt, y: UInt) = this[x.I, y.I]
      operator fun set(x: UInt, y: UInt, value: UInt) = set(x.I, y.I, value)
   }

   /**
    * Provides [Color] access to the canvas.
    */
   inline val C: ColorAccessor get() = ColorAccessor(this)
   @JvmInline value class ColorAccessor(private val canvas: BufferCanvas) {
      operator fun get(x: Int, y: Int) = canvas[x, y].RGBA.C
      operator fun set(x: Int, y: Int, color: Color) = canvas.set(x, y, color.RGBA.I)

      /**
       * Provides merged [Color] writing to the canvas.
       *
       * When setting a pixel to a semi-transparent color, it will be mixed with the color already in the canvas.
       * @see Color.paintOver
       */
      inline val M get() = MergeAccessor(this)
      @JvmInline value class MergeAccessor(private val color: ColorAccessor) {
         operator fun get(x: Int, y: Int) = color[x, y]
         operator fun set(x: Int, y: Int, added: Color) = color.set(x, y, added.paintOver(color[x, y]))
      }
   }

   /**
    * Provides direct [Byte] access to the backing [Uint8Buffer].
    *
    * For block operations use [toByteArray], [loadByteArray] or the [Int]/[UInt] variants.
    */
   inline val B: ByteAccessor get() = ByteAccessor(this)
   @JvmInline value class ByteAccessor(private val canvas: BufferCanvas) {
      private fun checkIndex(index: Int) {
         if (index < 0 || index >= canvas.bufferSize)
            throw IndexOutOfBoundsException("Canvas buffer index out of bounds: $index for size ${canvas.bufferSize}")
      }
      operator fun get(index: Int): Byte = checkIndex(index).let { canvas.buffer[index] }
      operator fun set(index: Int, value: Byte) = checkIndex(index).let {
         canvas.buffer[index] = value
      }
   }

   /**
    * Provides fenced access to the canvas buffer.
    *
    * Read operations outside the canvas bounds will always return 0, that is, transparent black.
    * Write operations outside the canvas will be ignored (and won't cause an update if [autoUpdate]
    * is `true`).
    *
    * Provides [U] and [C], fenced [UInt]/[Color] sub-accessors.
    */
   inline val F: FencedAccessor get() = FencedAccessor(this)
   @JvmInline value class FencedAccessor(internal val canvas: BufferCanvas) {
      operator fun get(index: Int) = if (canvas.validIndex(index)) canvas[index] else 0
      operator fun set(index: Int, value: Int) {
         if (canvas.validIndex(index)) canvas[index] = value
      }

      operator fun get(x: Int, y: Int): Int {
         val cx = x - canvas.origin.x
         val cy = y - canvas.origin.y
         return if (canvas.validIndex(cx, cy)) canvas[canvas.bufferIndex(cx, cy)] else 0
      }
      operator fun set(x: Int, y: Int, value: Int) {
         val cx = x - canvas.origin.x
         val cy = y - canvas.origin.y
         if (canvas.validIndex(cx, cy))
            canvas[canvas.bufferIndex(cx, cy)] = value
      }

      /**
       * Provides fenced [UInt] access to the canvas buffer.
       */
      inline val U get() = UnsignedAccessor(this)
      @JvmInline value class UnsignedAccessor(private val fenced: FencedAccessor) {
         operator fun get(index: Int) = fenced[index].U
         operator fun set(index: Int, value: UInt) = fenced.set(index, value.I)

         operator fun get(x: Int, y: Int) = fenced[x, y].U
         operator fun set(x: Int, y: Int, value: UInt) = fenced.set(x, y, value.I)
      }

      /**
       * Provides fenced [Color] access to the canvas buffer.
       */
      inline val C get() = ColorAccessor(this)
      @JvmInline value class ColorAccessor(private val fenced: FencedAccessor) {
         operator fun get(x: Int, y: Int) = fenced[x, y].RGBA.C
         operator fun set(x: Int, y: Int, color: Color) = fenced.set(x, y, color.RGBA.I)

         /**
          * Provides merged [Color] writing to the canvas.
          *
          * When setting a pixel to a semi-transparent color, it will be mixed with the color already in the canvas.
          * @see Color.paintOver
          */
         inline val M get() = MergeAccessor(this)
         @JvmInline value class MergeAccessor(private val color: ColorAccessor) {
            operator fun get(x: Int, y: Int) = color[x, y]
            operator fun set(x: Int, y: Int, added: Color) = color.set(x, y, added.paintOver(color[x, y]))
         }
      }

      /**
       * Provides direct [Byte] fenced access to the canvas buffer.
       */
      inline val B get() = ByteAccessor(this)
      @JvmInline value class ByteAccessor(private val fenced: FencedAccessor) {
         private fun valid(index: Int) = 0 <= index && index < fenced.canvas.bufferSize
         operator fun get(index: Int) = if (valid(index)) fenced.canvas.buffer[index] else 0
         operator fun set(index: Int, value: Byte) {
            if (valid(index)) fenced.canvas.buffer[index] = value
         }
      }
   }
}