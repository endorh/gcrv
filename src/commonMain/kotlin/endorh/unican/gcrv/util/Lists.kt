package endorh.unican.gcrv.util

fun <T> List<T>.toggling(e: T) = if (contains(e)) minus(e) else plus(e)
fun <T> MutableList<T>.toggle(e: T) = if (contains(e)) false.also { remove(e) } else add(e)