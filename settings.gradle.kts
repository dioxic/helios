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
        maven {
            url = uri("https://maven.pkg.github.com/dioxic/kotlinx-serialization-bson")
            credentials {
                val ghUsername: String? by settings
                val ghToken: String? by settings
                username = ghUsername
                password = ghToken
            }
        }
    }
}

include("generate")
include("cli")
include("execute")
