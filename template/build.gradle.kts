import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
    `with-docs`
}

dependencies {
    implementation(libs.bson)
//    implementation(libs.commons.math)
//    implementation(libs.arrow.core)
//    implementation(libs.arrow.optics)
//    implementation(libs.arrow.fx.coroutines)
    implementation(libs.kotlin.datetime)
//    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.faker)
    implementation(libs.kotlin.reflect)
    implementation(libs.reflections)
    implementation(libs.bundles.logging)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
}