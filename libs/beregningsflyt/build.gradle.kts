plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api(project(":libs:beregning"))
    api(project(":libs:oppdrag"))
    testImplementation(testFixtures(project(":libs:tid")))
}
