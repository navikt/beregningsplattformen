plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    implementation(project(":libs:beregning"))
    implementation(project(":libs:oppdrag"))
    testImplementation(project(":libs:beregningsflyt-verifikasjon"))
}
