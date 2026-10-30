plugins {
    kotlin("jvm")
    id("no.nav.ktlintgradle.ktlint")
    `java-library`
}

val ktorVersion = "3.6.0"

dependencies {
    api("org.slf4j:slf4j-api:2.0.18")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    api("io.ktor:ktor-client-logging:$ktorVersion")
}
