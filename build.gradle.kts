import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.serialization") version "2.4.21" apply false
    alias(libs.plugins.ktlint) apply false
}

val kotestVersion = "6.2.6"

subprojects {
    plugins.withId("no.nav.ktlintgradle.ktlint") {
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
        maven { url = uri("https://packages.confluent.io/maven/") }

        val githubToken = System.getenv("GITHUB_TOKEN")
        if (githubToken.isNullOrEmpty()) {
            maven {
                name = "external-mirror-github-navikt"
                url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
            }
        } else {
            maven {
                name = "github-package-registry-navikt"
                url = uri("https://maven.pkg.github.com/navikt/maven-release")
                credentials {
                    username = "token"
                    password = githubToken
                }
            }
        }
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
