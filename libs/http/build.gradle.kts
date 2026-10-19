plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

val ktorVersion = "3.6.0"

dependencies {
    api(project(":libs:log"))
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    api("io.ktor:ktor-client-logging:$ktorVersion")
}
