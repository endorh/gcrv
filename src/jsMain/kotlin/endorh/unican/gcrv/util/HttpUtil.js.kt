package endorh.unican.gcrv.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.job
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.coroutineContext

actual suspend fun httpGet(url: String): String {
   val request = XMLHttpRequest()
   request.open("GET", url, async=true)
   val result = CompletableDeferred<String>(coroutineContext.job)
   request.onload = {
      if (request.readyState == 4.S) {
         if (request.status == 200.S) {
            result.complete(request.responseText)
         } else {
            result.completeExceptionally(HttpRequestException(request.statusText, request.status.toInt()))
         }
      }
   }
   request.onerror = {
      result.completeExceptionally(HttpRequestException(request.statusText, request.status.toInt()))
   }
   request.send()
   return result.await()
}
