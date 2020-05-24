plugins {
    application
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.dokka") version "0.10.0"
}

group = "uk.dioxic"
version = "0.0.1-SNAPSHOT"

application {
    mainClassName = "uk.dioxic.mgenerate4k.app.MainKt"
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/dokka"
    configuration {
        moduleName = "data"
    }
}

//tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc") {
//    outputFormat = "javadoc"
//    outputDirectory = "$buildDir/javadoc"
//}

tasks.withType<Test> {
    if (!project.hasProperty("createReports")) {
        reports.html.isEnabled = false
        reports.junitXml.isEnabled = false
    }
}

tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

//tasks {
//    val dokkjavadoc by getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
//        outputFormat = "javadoc"
//        outputDirectory = "$buildDir/javadoc"
//    }
//}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
}
