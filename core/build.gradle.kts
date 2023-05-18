
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    `kotlin-conventions`
//    `with-test-fixtures`
    `with-docs`
}

group = "uk.dioxic"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":common"))
//    ksp(project(":processor"))

    implementation(libs.kbson)
    implementation(libs.bson)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.poet)
    implementation(libs.reflections)
//    implementation(libs.atomicfu)
    implementation(libs.bundles.logging)
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}