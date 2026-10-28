plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    application
}

group = "no.nav.utbetaling.beregning"

val ktorVersion = "3.6.0"
val micrometerVersion = "1.17.1"

dependencies {
    implementation(project(":libs:oppdrag"))
    implementation(project(":libs:tid")) // TODO: Hvorfor trenger jeg denne? Bør oppdrag ha en avhengighet på tid?
    implementation(project(":libs:http"))
    implementation(project(":libs:kotlinx"))
    implementation(project(":libs:log"))

    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
}

application {
    mainClass.set("no.nav.utbetaling.beregning.lab.LabKt")
}
