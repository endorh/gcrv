package endorh.unican.gcrv.ui2

import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asPromise
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Promise

object HTMLFileDialog {
   private const val INPUT_ELEMENT_ID = "---kotlin--html-file-dialog--input-impl"
   private const val DOWNLOAD_ELEMENT_ID = "---kotlin--html-file-dialog--download-impl"
   private val inputElement: HTMLInputElement by lazy {
      document.getElementById(INPUT_ELEMENT_ID) as? HTMLInputElement ?: run {
         (document.createElement("input") as HTMLInputElement).apply {
            id = INPUT_ELEMENT_ID
            type = "file"
            style.display = "none"
            onchange = {
               files?.get(0)?.let { file -> result?.complete(JsFileReadHandle(file)) }
                  ?: result?.complete(null)
            }
            document.body?.appendChild(this)
         }
      }
   }
   private val downloadElement: HTMLLinkElement by lazy {
      document.getElementById(DOWNLOAD_ELEMENT_ID) as? HTMLLinkElement ?: run {
         (document.createElement("a") as HTMLLinkElement).apply {
            id = DOWNLOAD_ELEMENT_ID
            style.display = "none"
            document.body?.appendChild(this)
         }
      }
   }
   private var result: CompletableDeferred<JsFileReadHandle?>? = null

   private fun setupInputFilters(filters: List<FileFilter>) {
      if (filters.isEmpty() || filters.any { it.extensions == "*" }) {
         inputElement.removeAttribute("accept")
      } else {
         inputElement.setAttribute("accept",
            filters.flatMap { it.extensions.split(',') }
               .map { if (it.startsWith('.')) it else ".$it" }.joinToString(","))
      }
   }

   fun loadSingleFile(filters: List<FileFilter> = listOf(FileFilter.ALL)): Promise<JsFileReadHandle?> {
      result?.let {
         it.cancel()
         result = null
      }
      return CompletableDeferred<JsFileReadHandle?>().also {
         result = it
         setupInputFilters(filters)
         inputElement.click()
      }.asPromise()
   }

   fun saveSingleTextFile(suggestedName: String, fileContent: String) {
      downloadElement.apply {
         href = "data:text/plain;charset=utf-8," + encodeURIComponent(fileContent)
         setAttribute("download", suggestedName)
         click()
      }
   }

   fun saveSingleBinaryFile(suggestedName: String, fileContent: ByteArray) {
      downloadElement.apply {
         val blob = Blob(fileContent.toTypedArray(), BlobPropertyBag(type = "application/octet-stream"))
         val url = URL.createObjectURL(blob)
         href = url
         setAttribute("download", suggestedName)
         click()
         URL.revokeObjectURL(url)
      }
   }
}

external fun encodeURIComponent(str: String): String