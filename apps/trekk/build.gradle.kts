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
    implementation(project(":libs:jdbc"))

    // Ktor
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.metrics.micrometer)

    // Database
    implementation(libs.postgresql)
    implementation(libs.kotliquery)

    // Micrometer
    implementation(libs.micrometer.registry.prometheus)

    // Tests
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktorVersion.get()}")
    testImplementation("io.mockk:mockk:1.14.12")
    testImplementation(libs.testcontainers.postgresql)
}

application {
    mainClass.set("no.nav.utbetaling.beregning.trekk.TrekkKt")
}
