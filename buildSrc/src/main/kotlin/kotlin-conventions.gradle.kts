import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.dioxic.gradle.libs

plugins {
    kotlin("jvm")
    id("base-conventions")
}

kotlin {
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.time.ExperimentalTime")
//                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
//                optIn("kotlinx.coroutines.FlowPreview")
//                optIn("kotlin.Experimental")
//                optIn("kotlin.RequiresOptIn")
//                optIn("kotlin.time.ExperimentalTime")
//                optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
//                optIn("kotlin.time.ExperimentalContracts")
            }
        }
    }
    jvmToolchain(17)
//    tasks.withType<KotlinJvmCompile> {
//        kotlinOptions.jvmTarget = "17"
//        kotlinOptions.languageVersion = "1.6"
//    }
//    tasks.withType<KotlinJvmTest>().configureEach {
//        useJUnitPlatform()
//        testLogging {
//            events("passed", "skipped", "failed")
//        }
//        maxParallelForks = 4
//    }
}

dependencies {
    implementation(libs.bundles.logging)
    testImplementation(libs.bundles.kotest)
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        freeCompilerArgs += "-Xcontext-receivers"
//    }
//}

project.tasks.withType<KotlinCompile>().forEach {
    it.kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
    maxParallelForks = 4
}
