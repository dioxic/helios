import uk.dioxic.gradle.libs

plugins {
    java
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(13))
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
    maxParallelForks = 4
}