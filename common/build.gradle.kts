
plugins {
    `kotlin-conventions`
}

group = "uk.dioxic"

dependencies {
    implementation(libs.bson)
    implementation(libs.kotlin.datetime)
}