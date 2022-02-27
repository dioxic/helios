
plugins {
    `kotlin-conventions`
}

group = "uk.dioxic"

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poetKsp)
    implementation(project(":common"))
}