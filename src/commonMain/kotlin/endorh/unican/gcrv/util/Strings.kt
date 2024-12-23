package endorh.unican.gcrv.util

private val titleCasePattern = Regex("""\b[a-z]|(?<=_)[a-z]""")
private val separatorPattern = Regex("""\s+|_|(?<=[a-z])(?=[A-Z])""")
fun String.toTitleCase() =
   replace(titleCasePattern) { it.value.uppercase() }
      .replace(separatorPattern, " ")


expect fun Float.format(digits: Int): String
fun Float.roundToString(decimals: Int = 3, noExp: Boolean = false): String {
   if (noExp) return format(decimals)
   val s = toString()
   if ('e' in s || 'E' in s) return s
   val i = s.lastIndexOf('.')
   return if (i > 0 && i < s.length - decimals - 1) s.substring(0, i + decimals + 1) else s
}
fun String.removeTrailingZeros() =
   if ('.' in this) removeSuffix("0").removeSuffix(".") else this

fun Int.pad(length: Int, pad: String = " "): String {
   val s = toString()
   return if (s.length < length) pad.repeat(length - s.length) + s else s
}

val Int.padLength: Int get() = toString().length