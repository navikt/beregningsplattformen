plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    application
}

group = "no.nav.utbetaling.beregning"

dependencies {
    implementation(project(":apps:beregningsplattform:mottak"))
    implementation(project(":apps:beregningsplattform:skattekort"))

    // Libs
    implementation(project(":libs:oppdrag"))
    implementation(project(":libs:log"))
    implementation(project(":libs:http"))
    implementation(project(":libs:jdbc"))
    implementation(project(":libs:kotlinx"))

    testImplementation(libs.kotliquery)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.extensions.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    // Ktor
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.metrics.micrometer)

    // Micrometer
    implementation(libs.micrometer.registry.prometheus)
}

application {
    mainClass.set("no.nav.utbetaling.beregning.beregningsplattform.ApplicationKt")
}
