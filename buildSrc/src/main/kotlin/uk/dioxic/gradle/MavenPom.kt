package uk.dioxic.gradle

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

internal fun MavenPublication.configurePom(
    project: Project,
) {
    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/dioxic/mgenerate4k")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("dioxic")
                name.set("Mark Baker-Munton")
                email.set("dioxic@gmail.com")
                organization.set("MongoDB")
                organizationUrl.set("https://www.mongodb.com/")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/dioxic/mgenerate4k.git")
            developerConnection.set("scm:git:git@github.com:dioxic/mgenerate4k.git")
            url.set("https://github.com/dioxic/mgenerate4k")
        }
    }
}
