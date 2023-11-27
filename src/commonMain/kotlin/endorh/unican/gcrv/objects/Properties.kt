package endorh.unican.gcrv.objects

import endorh.unican.gcrv.line_algorithms.ui.LineStyleProperty
import endorh.unican.gcrv.line_algorithms.ui.PointStyleProperty
import endorh.unican.gcrv.transformations.TransformProperty

typealias bool = BoolProperty
typealias string = StringProperty
typealias int = IntProperty
typealias float = FloatProperty
typealias double = DoubleProperty
typealias vec2i = Vec2iProperty
typealias vec2f = Vec2fProperty
typealias color = ColorProperty
typealias lineStyle = LineStyleProperty
typealias pointStyle = PointStyleProperty
typealias transform = TransformProperty

fun <T: Any> option(values: List<T>, value: T) = OptionProperty(values, value)
fun <T> option(values: List<T & Any>, value: T?) = NullableOptionProperty(values, value)