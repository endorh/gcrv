package endorh.unican.gcrv.util

actual fun Float.format(digits: Int) = asDynamic().toFixed(digits) as String