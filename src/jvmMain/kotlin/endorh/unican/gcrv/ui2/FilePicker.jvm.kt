package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CompletableDeferred
import java.io.File

actual open class FilePickerNode actual constructor(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), FilePickerScope {
   override val modifier: FilePickerModifier = FilePickerModifier(surface)
   val path = mutableStateOf("")
   private var label: String = "Browse"

   actual fun setup(label: String) {
      this.label = label
   }

   override fun applyDefaults() {
      super.applyDefaults()

      val mod = modifier
      Row(Grow.Std) {
         if (mod.showFileName) BlendTextField(path.use()) {
            modifier.width(Grow.Std).textAlignX(AlignmentX.End).onChange { path ->
               this@FilePickerNode.path.value = path
               File(path).takeIf { it.isFile }?.let { file ->
                  mod.onFileChosen?.invoke(JvmFileReadHandle(file))
               }
            }
         }

         Button(label) {
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
   private var label: String = "Save"

   actual fun setup(label: String) {
      this.label = label
   }

   override fun applyDefaults() {
      super.applyDefaults()

      val mod = modifier
      Row(Grow.Std) {
         if (mod.showFileName) {
            BlendTextField(mod.suggestedFileName) {
               modifier.width(Grow.Std).textAlignX(AlignmentX.End).onChange {
                  mod.suggestedFileName = it
               }
            }
         }

         Button(label) {
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
