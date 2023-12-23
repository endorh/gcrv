package endorh.unican.gcrv.util

expect suspend fun httpGet(url: String): String

open class HttpRequestException(message: String, val code: Int? = null) : RuntimeException(message)