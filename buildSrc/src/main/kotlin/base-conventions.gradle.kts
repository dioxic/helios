import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions")
}

version = "0.0.1-SNAPSHOT"

tasks.withType<DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                    reject("Release candidate")
                }
            }
        }
    }
}


repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/dioxic/kotlinx-serialization-bson")
        credentials {
            val ghUsername: String? by project
            val ghToken: String? by project
            username = ghUsername
            password = ghToken
        }
    }
}


fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}