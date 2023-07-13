@file:Suppress("UnstableApiUsage")

rootProject.name = "helios"


pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("generate")
include("cli")
include("execute")
