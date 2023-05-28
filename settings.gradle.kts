@file:Suppress("UnstableApiUsage")

rootProject.name = "mgenerate4k"


pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    val kotlinVersion: String by settings
    val atomicfuVersion: String by settings
//    resolutionStrategy {
//        eachPlugin {
//            when {
//                requested.id.id == "kotlinx-atomicfu" ->
//                    useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicfuVersion")
//                requested.id.id.startsWith("org.jetbrains.kotlin") ->
//                    useVersion(kotlinVersion)
//            }
//        }
//    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
include("core")
