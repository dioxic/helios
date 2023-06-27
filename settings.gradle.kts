@file:Suppress("UnstableApiUsage")

rootProject.name = "helios"


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
include("generate")
include("cli")
include("execute")
