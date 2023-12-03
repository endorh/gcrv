package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CompletableDeferred
import java.io.File

actual open class FilePickerNode actual constructor(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), FilePickerScope {
   override val modifier: FilePickerModifier = FilePickerModifier(surface)
   val path = mutableStateOf("")

   override fun applyDefaults() {
      super.applyDefaults()

      val mod = modifier
      Row(Grow.Std) {
         BlendTextField(path.use()) {
            modifier.width(Grow.Std).textAlignX(AlignmentX.End).onChange { path ->
               this@FilePickerNode.path.value = path
               File(path).takeIf { it.isFile }?.let { file ->
                  mod.onFileChosen?.invoke(JvmFileReadHandle(file))
               }
            }
         }

         Button("Browse") {
            onClick {
               NativeFileDialog.loadSingleFile(mod.fileFilters.map {
                  NativeFileDialog.FileFilter(it.description, it.extensions)
               })?.let {
                  path.value = it.path
                  mod.onFileChosen?.invoke(JvmFileReadHandle(it))
               }
            }
         }
      }
   }

   actual companion object {
      actual val factory: (UiNode?, UiSurface) -> FilePickerNode = ::FilePickerNode
   }
}

actual open class FileSaverNode actual constructor(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), FileSaverScope {
   override val modifier = FileSaverModifier(surface)

   override fun applyDefaults() {
      super.applyDefaults()

      val mod = modifier
      Row(Grow.Std) {
         BlendTextField(mod.suggestedFileName) {
            modifier.width(Grow.Std).textAlignX(AlignmentX.End).onChange {
               mod.suggestedFileName = it
            }
         }

         Button("Save") {
            onClick {
               mod.onFileRequested?.invoke()?.let { contents ->
                  NativeFileDialog.saveSingleFile(mod.suggestedFileName, mod.fileFilters.map {
                     NativeFileDialog.FileFilter(it.description, it.extensions)
                  })?.let {
                     when (contents) {
                        is FileWriteContents.StringContents -> it.writeText(contents.content)
                        is FileWriteContents.ByteArrayContents -> it.writeBytes(contents.content)
                     }
                  }
               }
            }
         }
      }
   }

   actual companion object {
      actual val factory: (UiNode?, UiSurface) -> FileSaverNode = ::FileSaverNode
   }
}

class JvmFileReadHandle(val file: File) : FileReadHandle {
   override val name get() = file.name
   override fun readAsText() = CompletableDeferred(file.readText())
   override fun readAsByteArray() = CompletableDeferred(file.readBytes())
}
