import uk.dioxic.gradle.libs

plugins {
    java
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
    maxParallelForks = 4
}