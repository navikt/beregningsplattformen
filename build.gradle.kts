import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

val kotestVersion = "6.2.5"

subprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        tasks.named("build") {
            dependsOn("ktlintFormat")
        }
        tasks.withType<KtLintCheckTask>().configureEach {
            dependsOn("ktlintFormat")
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(25)
        }

        tasks.withType<KotlinCompile> {
            compilerOptions.jvmTarget = JVM_25
        }

        dependencies {
            add("testImplementation", "io.kotest:kotest-assertions-core-jvm:$kotestVersion")
            add("testImplementation", "io.kotest:kotest-runner-junit5:$kotestVersion")
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
