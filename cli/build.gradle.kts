import uk.dioxic.gradle.libs

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-conventions`
    application
//    `with-test-fixtures`
    `with-docs`
    distribution
}

dependencies {
    implementation(project(":generate"))
    implementation(project(":execute"))
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.bson)
    implementation(libs.mongodb.sync)
    implementation(libs.bundles.logging)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.bson)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.clikt)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("uk.dioxic.helios.cli.CliKt")
}

distributions {
    main {
        distributionBaseName.set("cli")
    }
}