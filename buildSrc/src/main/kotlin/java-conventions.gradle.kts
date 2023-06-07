import uk.dioxic.gradle.libs

plugins {
    java
}

dependencies {
    implementation(libs.bundles.logging)
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
    maxParallelForks = 4
}