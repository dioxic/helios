import uk.dioxic.gradle.libs

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
    `with-docs`
}

dependencies {
    implementation(libs.bson)
    implementation(libs.commons.math)
    implementation(libs.mongodb.sync)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.faker)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)
    implementation(libs.bundles.logging)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
}