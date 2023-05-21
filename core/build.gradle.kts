
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
//    `with-test-fixtures`
    `with-docs`
}

group = "uk.dioxic"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":common"))
//    ksp(project(":processor"))

    implementation(libs.bson)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)
    implementation(libs.bundles.logging)
}