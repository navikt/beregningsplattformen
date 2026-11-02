
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
    id("publiser-bibliotek")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}

tasks.named<Test>("test") {
    dependsOn(tasks.named("jar"))
}
