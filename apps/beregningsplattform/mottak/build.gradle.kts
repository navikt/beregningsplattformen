plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

group = "no.nav.utbetaling.beregning"

dependencies {
    api(libs.micrometer.registry.prometheus)
    api(project(":libs:mq"))
    api(project(":libs:oppdrag"))
    implementation(project(":libs:log"))
}
