plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api(libs.hikaricp)
    api(libs.flyway.core)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(libs.h2)
}
