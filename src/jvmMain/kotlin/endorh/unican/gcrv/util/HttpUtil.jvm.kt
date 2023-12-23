package endorh.unican.gcrv.util

import kotlinx.coroutines.future.asDeferred
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val HTTP_Client = HttpClient.newBuilder().build()
private val HTTP_GET_RequestBuilder = HttpRequest.newBuilder()

actual suspend fun httpGet(url: String): String {
   val request = HTTP_GET_RequestBuilder
      .uri(URI.create(url))
      .build()
   val response = HTTP_Client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
   return response.asDeferred().await().body()
}