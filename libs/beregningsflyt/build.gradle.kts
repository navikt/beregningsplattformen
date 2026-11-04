plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    implementation(project(":libs:beregning"))
    implementation(project(":libs:oppdrag"))
    testImplementation(testFixtures(project(":libs:tid")))
}
