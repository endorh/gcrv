package endorh.unican.gcrv.ui2

import de.fabmax.kool.modules.ui2.UiModifier
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.UiSurface
import kotlinx.coroutines.Deferred
import kotlin.experimental.ExperimentalTypeInference

/**
 *
 */
data class FileFilter(val description: String, val extensions: String) {
   companion object {
      val ALL = FileFilter("All files", "*")
      val TEXT = FileFilter("Text files", "txt")
      val IMAGE = FileFilter("Images", "jpg,jpeg,png,gif,bmp")
      val SVG = FileFilter("SVG files", "svg")
      val JSON = FileFilter("JSON files", "json")
      val XML = FileFilter("XML files", "xml")
      val HTML = FileFilter("HTML files", "html,htm")
      val JS = FileFilter("JavaScript files", "js")
      val CSS = FileFilter("CSS files", "css")
      val OBJ = FileFilter("OBJ files", "obj")
      val GLTF = FileFilter("glTF files", "gltf,glb")

      val KOTLIN = FileFilter("Kotlin files", "kt")
      val KOTLIN_SCRIPT = FileFilter("Kotlin script files", "kts")
      val KOTLIN_ANY = FileFilter("Any Kotlin files", "kt,kts")
   }
}

interface FilePickerScope : UiScope {
   override val modifier: FilePickerModifier
}

interface FileSaverScope : UiScope {
   override val modifier: FileSaverModifier
}

open class FilePickerModifier(surface: UiSurface) : UiModifier(surface) {
   var showFileName: Boolean by property(false)
   var dialogTitle: String by property("Choose file")
   var fileFilters: List<FileFilter> by property(listOf(FileFilter.ALL))
   var onFileChosen: ((FileReadHandle) -> Unit)? by property(null)
}

open class FileSaverModifier(surface: UiSurface) : UiModifier(surface) {
   var showFileName: Boolean by property(false)
   var dialogTitle: String by property("Save file")
   var fileFilters: List<FileFilter> by property(listOf(FileFilter.ALL))
   var suggestedFileName: String by property("")
   var onFileRequested: (() -> FileWriteContents?)? by property(null)
}

fun <T: FilePickerModifier> T.showFileName(showFileName: Boolean = true) = apply { this.showFileName = false }
fun <T: FilePickerModifier> T.dialogTitle(title: String) = apply { dialogTitle = title }
fun <T: FilePickerModifier> T.fileFilters(vararg filters: FileFilter) = apply { fileFilters = filters.toList() }
fun <T: FilePickerModifier> T.fileFilters(filters: List<FileFilter>) = apply { fileFilters = filters }
fun <T: FilePickerModifier> T.onFileChosen(block: ((FileReadHandle) -> Unit)? = null) = apply { onFileChosen = block }

fun <T: FileSaverModifier> T.showFileName(showFileName: Boolean = true) = apply { this.showFileName = false }
fun <T: FileSaverModifier> T.dialogTitle(title: String) = apply { dialogTitle = title }
fun <T: FileSaverModifier> T.suggestedFileName(name: String) = apply { suggestedFileName = name }
fun <T: FileSaverModifier> T.fileFilters(vararg filters: FileFilter) = apply { fileFilters = filters.toList() }
fun <T: FileSaverModifier> T.fileFilters(filters: List<FileFilter>) = apply { fileFilters = filters }
fun <T: FileSaverModifier> T.onFileRequested(block: (() -> FileWriteContents?)? = null) = apply { onFileRequested = block }

fun FilePickerScope.onFileChosen(block: (FileReadHandle) -> Unit) = modifier.onFileChosen(block)
fun FileSaverScope.onFileTextRequested(block: () -> String?) = modifier.onFileRequested {
   block()?.let { FileWriteContents.StringContents(it) }
}
fun FileSaverScope.onFileDataRequested(block: () -> ByteArray?) = modifier.onFileRequested {
   block()?.let { FileWriteContents.ByteArrayContents(it) }
}

inline fun UiScope.FilePicker(label: String = "Browse", scopeName: String? = null, block: FilePickerScope.() -> Unit = {}): FilePickerScope =
   uiNode.createChild(scopeName, FilePickerNode::class, FilePickerNode.factory).apply {
      setup(label)
      block()
   }

inline fun UiScope.FileSaver(label: String = "Save", scopeName: String? = null, block: FileSaverScope.() -> Unit = {}): FileSaverScope =
   uiNode.createChild(scopeName, FileSaverNode::class, FileSaverNode.factory).apply {
      setup(label)
      block()
   }

expect open class FilePickerNode(parent: UiNode?, surface: UiSurface) : UiNode, FilePickerScope {
   companion object {
      val factory: (UiNode?, UiSurface) -> FilePickerNode
   }

   fun setup(label: String)
}

expect open class FileSaverNode(parent: UiNode?, surface: UiSurface) : UiNode, FileSaverScope {
   companion object {
      val factory: (UiNode?, UiSurface) -> FileSaverNode
   }

   fun setup(label: String)
}

interface FileReadHandle {
   val name: String

   fun readAsText(): Deferred<String>
   fun readAsByteArray(): Deferred<ByteArray>
}

sealed interface FileWriteContents {
   data class StringContents(val content: String) : FileWriteContents
   data class ByteArrayContents(val content: ByteArray) : FileWriteContents
}