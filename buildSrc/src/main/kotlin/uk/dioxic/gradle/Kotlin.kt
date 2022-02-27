package uk.dioxic.gradle

import org.gradle.api.Project

internal val Project.isKotlinMultiplatformProject: Boolean
    get() = plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

internal val Project.isKotlinJvmProject: Boolean
    get() = plugins.hasPlugin("org.jetbrains.kotlin.jvm")

internal val Project.isPlatformProject: Boolean
    get() = plugins.hasPlugin("java-platform")

internal val Project.isJavaLibraryProject: Boolean
    get() = plugins.hasPlugin("java-library")

