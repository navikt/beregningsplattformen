plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api(libs.jdbi.core)
    api(libs.jdbi.kotlin)
    api(libs.kotlin.logging)
    api(libs.opentelemetry.api)

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.hikaricp)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    // Testene deler én Postgres-container; kjør dem sekvensielt.
    maxParallelForks = 1
    systemProperty("kotest.framework.parallelism", "1")
    systemProperty("kotest.framework.concurrency.mode", "sequential")
}
