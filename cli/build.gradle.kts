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
}

tasks.test {
    useJUnitPlatform()
}