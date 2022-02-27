import uk.dioxic.gradle.configurePom
import uk.dioxic.gradle.isJavaLibraryProject
import uk.dioxic.gradle.isKotlinMultiplatformProject
import uk.dioxic.gradle.isPlatformProject

plugins {
    `maven-publish`
    `java-library`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

val javadocJar = if (isKotlinMultiplatformProject) {
    tasks.register("emptyJavadocJar", Jar::class) {
        archiveClassifier.set("javadoc")
    }
} else null

configure<PublishingExtension> {
    publications {
        when {
            isJavaLibraryProject ->
                withType<MavenPublication>().configureEach {
                    groupId = project.group.toString()
                    artifactId = "${project.name}-$name"
                    version = "${project.version}"

                    if (name == "jvm") {
                        artifact(javadocJar!!.get())
                    }

                    configurePom(project)
                }

            isPlatformProject ->
                create<MavenPublication>("bom") {
                    from(components["javaPlatform"])

                    groupId = project.group.toString()
                    artifactId = project.name
                    version = "${project.version}"

                    configurePom(project)
                }

            else ->
                println("Unknown publication")
        }
    }

    repositories {
        maven {
            name = "kotlinSpace"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}

signing {
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })

    sign(publishing.publications)
}
