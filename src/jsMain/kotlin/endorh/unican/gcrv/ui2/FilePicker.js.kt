package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.files.File
import org.w3c.files.FileReader

actual open class FilePickerNode actual constructor(parent: UiNode?, surface: UiSurface) : UiNode(parent, surface), FilePickerScope {
   override val modifier = FilePickerModifier(surface)
   private val path = mutableStateOf("")

   override fun applyDefaults() {
      super.applyDefaults()

      val mod = modifier
      Row(Grow.Std) {
         BlendTextField(path.use()) {
            modifier.isEditable(false).width(Grow.Std)
         }

         Button("Browse") {
            modifier.onClick {
               HTMLFileDialog.loadSingleFile(mod.fileFilters).then {
                  it?.let {
                     path.value = it.name
                     mod.onFileChosen?.invoke(it)
                  }
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
         Button("Save") {
            modifier.width(Grow.Std).onClick {
               mod.onFileRequested?.invoke()?.let { contents ->
                  when (contents) {
                     is FileWriteContents.StringContents ->
                        HTMLFileDialog.saveSingleTextFile(mod.suggestedFileName, contents.content)
                     is FileWriteContents.ByteArrayContents ->
                        HTMLFileDialog.saveSingleBinaryFile(mod.suggestedFileName, contents.content)
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

class JsFileReadHandle(val file: File) : FileReadHandle {
   override val name get() = file.name
   override fun readAsText() = CompletableDeferred<String>().apply {
      FileReader().apply {
         onload = {
            (result as String?)?.let { complete(it) } ?: completeExceptionally(
               IllegalStateException("Failed to read file: $name")
            )
         }
         readAsText(file)
      }
   }

   override fun readAsByteArray(): Deferred<ByteArray> = CompletableDeferred<ByteArray>().apply {
      FileReader().apply {
         onload = {
            (result as ArrayBuffer?)?.let {
               complete(Int8Array(it).unsafeCast<ByteArray>())
            } ?: completeExceptionally(
               IllegalStateException("Failed to read file: $name")
            )
         }
         readAsArrayBuffer(file)
      }
   }
}
