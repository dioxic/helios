import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    `kotlin-conventions`
    `with-docs`
}

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
}

dependencies {
    implementation(project(":generate"))
    ksp(libs.arrow.optics.ksp.plugin)
    implementation(libs.okio.core)
    implementation(libs.bson)
    implementation(libs.mongodb.sync)
    implementation(libs.commons.math)
    implementation(libs.arrow.core)
    implementation(libs.arrow.optics)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.resilience)
    implementation(libs.kotlin.datetime)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.bson)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.bundles.logging)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okio.fakefilesystem)
}