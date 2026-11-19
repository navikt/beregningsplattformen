plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

group = "no.nav.utbetaling.beregning"

dependencies {
    api(libs.ktor.server.core)
    testImplementation(libs.ktor.server.test.host)
}
