package endorh.unican.gcrv.ui2

import de.fabmax.kool.util.Log
import de.fabmax.kool.util.logE
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.nfd.NFDFilterItem
import org.lwjgl.util.nfd.NFDPathSetEnum
import org.lwjgl.util.nfd.NativeFileDialog.*
import java.io.File
import java.nio.ByteBuffer


object NativeFileDialog {
   data class FileFilter(val name: String, val spec: String)

   fun loadSingleFile(
      filters: List<FileFilter> = listOf(FileFilter("All files", "*"))
   ) = stackPush().use { stack ->
      val filterBuffer = NFDFilterItem.malloc(filters.size)
      for ((i, f) in filters.withIndex())
         filterBuffer[i].name(stack.UTF8(f.name)).spec(stack.UTF8(f.spec))
      val pp = stack.mallocPointer(1)
      when (NFD_OpenDialog(pp, filterBuffer, null as ByteBuffer?)) {
         NFD_OKAY -> File(pp.getStringUTF8(0)).also { NFD_FreePath(pp[0]) }
         NFD_CANCEL -> null
         else -> null.also { Log.logE { "Error: ${NFD_GetError()}" } }
      }
   }

   fun loadMultipleFiles(
      filters: List<FileFilter> = listOf(FileFilter("All files", "*"))
   ): List<File>? = stackPush().use { stack ->
      val filterBuffer = NFDFilterItem.malloc(filters.size)
      for ((i, f) in filters.withIndex())
         filterBuffer[i].name(stack.UTF8(f.name)).spec(stack.UTF8(f.spec))
      val pp = stack.mallocPointer(1)
      val result = NFD_OpenDialogMultiple(pp, filterBuffer, null as ByteBuffer?)
      when (result) {
         NFD_OKAY -> mutableListOf<File>().apply {
            val pathSet = pp[0]
            val psEnum = NFDPathSetEnum.calloc(stack)
            NFD_PathSet_GetEnum(pathSet, psEnum)
            while (NFD_PathSet_EnumNext(psEnum, pp) == NFD_OKAY && pp[0] != NULL) {
               add(File(pp.getStringUTF8(0)))
               NFD_PathSet_FreePath(pp[0])
            }
            NFD_PathSet_FreeEnum(psEnum)
            NFD_PathSet_Free(pathSet)
         }
         NFD_CANCEL -> null
         else -> null.also { Log.logE { "Error: ${NFD_GetError()}" } }
      }
   }

   fun openFolder() = stackPush().use { stack ->
      val outPath = stack.mallocPointer(1)
      when (NFD_PickFolder(outPath, null as ByteBuffer?)) {
         NFD_OKAY -> File(outPath.getStringUTF8(0)).also { NFD_FreePath(outPath[0]) }
         NFD_CANCEL -> null
         else -> null.also { Log.logE { "Error: ${NFD_GetError()}" } }
      }
   }

   fun saveSingleFile(
      name: String, filters: List<FileFilter> = listOf(FileFilter("All files", "*"))
   ) = stackPush().use { stack ->
      val filterBuffer = NFDFilterItem.malloc(filters.size)
      for ((i, f) in filters.withIndex())
         filterBuffer[i].name(stack.UTF8(f.name)).spec(stack.UTF8(f.spec))
      val pp = stack.mallocPointer(1)
      when (NFD_SaveDialog(pp, filterBuffer, null, name)) {
         NFD_OKAY -> File(pp.getStringUTF8(0)).also { NFD_FreePath(pp[0]) }
         NFD_CANCEL -> null
         else -> null.also { Log.logE { "Error: ${NFD_GetError()}" } }
      }
   }
}
