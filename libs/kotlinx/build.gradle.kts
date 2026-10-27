plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}
