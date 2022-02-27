import org.gradle.internal.impldep.junit.runner.Version.id

/**
 * A plugin for adding dokka support for documentation generation.
 */
plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml.configure {
    enabled = true
}

tasks.dokkaJavadoc.configure {
    enabled = true
}