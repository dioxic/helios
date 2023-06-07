import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
//    `with-test-fixtures`
    `with-docs`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(libs.bson)
    implementation(libs.mongodb.sync)
    implementation(libs.bundles.logging)
    implementation(libs.clikt)
}

tasks.test {
    useJUnitPlatform()
}