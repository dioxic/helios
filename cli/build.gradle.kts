import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
    application
//    `with-test-fixtures`
    `with-docs`
    distribution
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(libs.bson)
    implementation(libs.mongodb.sync)
    implementation(libs.bundles.logging)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.clikt)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("uk.dioxic.mgenerate.cli.CliKt")
}

distributions {
    main {
        distributionBaseName.set("cli")
    }
}