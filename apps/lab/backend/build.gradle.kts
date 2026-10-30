plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("no.nav.ktlintgradle.ktlint")
    application
}

group = "no.nav.utbetaling.beregning"

dependencies {
    implementation(project(":libs:oppdrag"))
    implementation(project(":libs:http"))
    implementation(project(":libs:kotlinx"))
    implementation(project(":libs:log"))

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.metrics.micrometer)

    // Micrometer
    implementation(libs.micrometer.registry.prometheus)
}

application {
    mainClass.set("no.nav.utbetaling.beregning.lab.LabKt")
}
