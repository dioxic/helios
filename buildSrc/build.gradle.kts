import java.util.*

plugins {
    `kotlin-dsl`
//    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val props = Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}

val gradleNexusVersion = "1.1.0"

dependencies {
    /**
     * Workaround to make version catalogs accessible from precompiled script plugins
     * https://github.com/gradle/gradle/issues/15383
     */
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation("org.jetbrains.kotlin", "kotlin-gradle-plugin", libs.versions.kotlin.get())
    implementation("com.github.ben-manes", "gradle-versions-plugin", libs.versions.versionsPlugin.get())
    implementation("org.jetbrains.dokka", "dokka-gradle-plugin", libs.versions.dokka.get())
    implementation("io.github.gradle-nexus", "publish-plugin", gradleNexusVersion)
}