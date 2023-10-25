import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlin {
    jvm {
        jvmToolchain(11)
    }
    js(IR) {
        binaries.executable()
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution(Action {
                outputDirectory.set(File("${rootDir}/dist/${project.name}"))
            })
            commonWebpackConfig(Action {
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            })
        }
    }

    sourceSets {
        val koolVersion = "0.12.1"            // latest stable version
        // val koolVersion = "0.13.0-SNAPSHOT"   // newer but minor breaking changes might occur from time to time

        val lwjglVersion = "3.3.3"
        val physxJniVersion = "2.3.1"

        // JVM target platforms, you can remove entries from the list in case you want to target
        // only a specific platform
        val targetPlatforms = listOf("natives-windows", "natives-linux", "natives-macos")

        val commonMain by getting {
            dependencies {
                implementation("de.fabmax.kool:kool-core:$koolVersion")
                implementation("de.fabmax.kool:kool-physics:$koolVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                implementation("org.jetbrains.kotlin:kotlin-scripting-common")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

                // Runtime librares
                for (platform in targetPlatforms) {
                    // lwjgl runtime libs
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$platform")
                    listOf("glfw", "opengl", "jemalloc", "nfd", "stb", "vma", "shaderc").forEach { lib ->
                        runtimeOnly("org.lwjgl:lwjgl-$lib:$lwjglVersion:$platform")
                    }

                    // physx-jni runtime libs
                    runtimeOnly("de.fabmax:physx-jni:$physxJniVersion:$platform")

                    // use this for CUDA acceleration of physics demos (remove above physxJniRuntime dependency)
                    //   requires CUDA enabled physx-jni library in directory kool-demo/libs
                    //   grab the library from GitHub releases: https://github.com/fabmax/physx-jni/releases
                    //   also make sure to use the same version as used by kool-physics
                    //runtimeOnly(files("${projectDir}/libs/physx-jni-natives-windows-cuda-2.2.1.jar"))
                }
            }
        }

        val jsMain by getting {
            dependencies { }
        }

        sourceSets.all {
            languageSettings.apply {
                progressiveMode = true
            }
        }
    }
}

tasks["clean"].doLast {
    delete("${rootDir}/dist/${project.name}")
}

tasks.register<JavaExec>("run") {
    group = "application"
    mainClass.set("de.fabmax.kool.demo.MainKt")

    kotlin {
        val main = targets["jvm"].compilations["main"]
        dependsOn(main.compileAllTaskName)
        classpath(
            { main.output.allOutputs.files },
            { configurations["jvmRuntimeClasspath"] }
        )
    }
}