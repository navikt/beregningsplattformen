import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

val kotestVersion = "6.2.5"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    tasks {
        withType<KotlinCompile> {
            compilerOptions.jvmTarget = JVM_25
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }

    sourceSets {
        main {
            kotlin.srcDir("main")
            resources.srcDir("main")
        }
        test {
            kotlin.srcDir("test")
            resources.srcDir("test")
        }
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    kotlin {
        jvmToolchain(25)
    }

    dependencies {
        testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    }
}
