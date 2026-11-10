plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

val ktorVersion = "3.6.1"

dependencies {
    api("org.slf4j:slf4j-api:2.0.20")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    api("io.ktor:ktor-client-logging:$ktorVersion")
}
