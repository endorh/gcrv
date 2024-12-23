package endorh.unican.gcrv.util

import java.util.*

actual fun Float.format(digits: Int) = "%.${digits}f".format(Locale.US, this)