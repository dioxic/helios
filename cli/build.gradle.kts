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
    implementation(libs.clikt)
}

tasks.test {
    useJUnitPlatform()
}