plugins {
    kotlin("jvm")
    id("no.nav.ktlintgradle.ktlint")
    `java-library`
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}
