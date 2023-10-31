package de.fabmax.kool.demo

import de.fabmax.kool.KoolContext
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.MdColor
import endorh.unican.gcrv.LineAlgorithmsScene
import kotlin.math.max

object Scenes {

    val sceneColors = ColorGradient(
        0f to MdColor.AMBER,
        0.2f to MdColor.DEEP_ORANGE,

        0.25f to MdColor.PINK,
        0.45f to MdColor.PURPLE,

        0.5f to MdColor.INDIGO,
        0.6f to MdColor.BLUE,
        0.7f to MdColor.CYAN,

        0.8f to MdColor.GREEN,
        0.9f to MdColor.LIGHT_GREEN,
        0.95f to MdColor.LIME,
        1f to MdColor.LIME,
        n = 512
    )

    val practiceScenes = Category("Practice", false, 0f, 0.15f).apply {
        entry("line-algorithms", "Line Algorithms") { LineAlgorithmsScene() }
    }

    // val physicsDemos = Category("Physics", false, 0.2f, 0.35f).apply {
    //     entry("phys-terrain", "Island") { TerrainDemo() }
    //     entry("phys-vehicle", "Vehicle") { VehicleDemo() }
    //     entry("phys-ragdoll", "Ragdolls") { RagdollDemo() }
    //     entry("phys-joints", "Joints") { JointsDemo() }
    //     entry("physics", "Collision") { CollisionDemo() }
    // }

    // val graphicsDemos = Category("Graphics", false, 0.4f, 0.5f).apply {
    //     entry("ao", "Ambient Occlusion") { AoDemo() }
    //     entry("gltf", "glTF Models") { GltfDemo() }
    //     entry("ssr", "Reflections") { ReflectionDemo() }
    //     entry("deferred", "Deferred Shading") { DeferredDemo() }
    //     entry("procedural", "Procedural Roses") { ProceduralDemo() }
    //     entry("pbr", "PBR Materials") { PbrDemo() }
    // }

    // val techDemos = Category("Tech", false, 0.55f, 0.7f).apply {
    //     entry("creative-coding", "Creative Coding") { CreativeCodingDemo() }
    //     entry("instance", "Instanced Drawing") { InstanceDemo() }
    //     entry("bees", "Fighting Bees") { BeeDemo() }
    //     entry("simplification", "Simplification") { SimplificationDemo() }
    //     entry("ui", "User Interface") { UiDemo() }
    // }

    // val hiddenDemos = Category("Hidden", true, 0.75f, 0.95f).apply {
    //     entry("helloworld", "Hello World") { HelloWorldDemo() }
    //     entry("hellogltf", "Hello glTF") { HelloGltfDemo() }
    //     entry("hellobuffers", "Hello RenderToTexture") { HelloRenderToTextureDemo() }
    //     entry("hello-ui", "Hello UI") { HelloUiDemo() }
    //     entry("manybodies", "Many Bodies") { ManyBodiesDemo() }
    //     entry("manyvehicles", "Many Vehicles") { ManyVehiclesDemo() }
    //     entry("ksl-test", "Ksl Shading Test") { KslShaderTest() }
    //     entry("gizmo-test", "Gizmo Test") { GizmoTest() }
    // }

    val categories = mutableListOf(practiceScenes, /*physicsDemos, graphicsDemos, techDemos, hiddenDemos*/)
    val scenes = categories.flatMap { it.entries }.associateBy { it.id }.toMutableMap()

    val defaultScene = "line-algorithms"

    class Category(val title: String, val isHidden: Boolean, val fromColor: Float, val toColor: Float) {
        val entries = mutableListOf<Entry>()

        fun getCategoryColor(f: Float): Color {
            return sceneColors.getColor(fromColor + f * (toColor - fromColor))
        }

        fun entry(id: String, title: String, factory: (KoolContext) -> SimpleScene) {
            entries += Entry(this, id, title, factory)
        }
    }

    class Entry(val category: Category, val id: String, val title: String, val newInstance: (KoolContext) -> SimpleScene) {
        val color: Color
            get() {
                val catIdx = max(0, category.entries.indexOf(this)).toFloat()
                val gradientF = catIdx / category.entries.lastIndex
                return category.getCategoryColor(gradientF)
            }
    }
}