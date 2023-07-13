import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
    `with-docs`
}

dependencies {
    implementation(libs.bson)
    implementation(libs.arrow.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.bson)
    implementation(libs.faker)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)
    implementation(libs.bundles.logging)
    testImplementation(libs.kotest.arrow.assertions)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
}