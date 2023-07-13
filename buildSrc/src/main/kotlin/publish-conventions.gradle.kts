import uk.dioxic.gradle.*

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
                    configureVersionMapping()
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

        repositories {
            maven {
                val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
                val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            }
        }
    }
}

signing {
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })

    sign(publishing.publications)
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}