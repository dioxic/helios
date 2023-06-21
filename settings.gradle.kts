@file:Suppress("UnstableApiUsage")

rootProject.name = "mgenerate4k"


pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
include("template")
include("cli")
include("execute")
