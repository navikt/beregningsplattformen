plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    application
}

group = "no.nav.utbetaling.beregning"

dependencies {
    // Libs
    implementation(project(":libs:oppdrag"))
    implementation(project(":libs:log"))
    implementation(project(":libs:http"))
    implementation(project(":libs:kotlinx"))
    implementation(project(":libs:mq"))

    // Ktor
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.metrics.micrometer)

    // Micrometer
    implementation(libs.micrometer.registry.prometheus)
}

application {
    mainClass.set("no.nav.utbetaling.beregning.mottak.MottakKt")
}
