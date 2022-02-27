import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import uk.dioxic.gradle.libs

plugins {
    kotlin("jvm")
//    alias(libs.plugins.versions)
    id("base-conventions")
}

kotlin {
    sourceSets {
        all {
            languageSettings.apply {
//                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
//                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlin.Experimental")
                optIn("kotlin.time.ExperimentalTime")
//                optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
//                optIn("kotlin.time.ExperimentalContracts")
            }
        }
    }
    tasks.withType<KotlinJvmCompile> {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.languageVersion = "1.6"
    }
//    tasks.withType<KotlinJvmTest>().configureEach {
//        useJUnitPlatform()
//        testLogging {
//            events("passed", "skipped", "failed")
//        }
//        maxParallelForks = 4
//    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
    maxParallelForks = 4
}
