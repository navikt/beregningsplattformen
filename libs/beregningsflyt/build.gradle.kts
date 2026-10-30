plugins {
    kotlin("jvm")
    id("no.nav.ktlintgradle.ktlint")
    `java-library`
}

dependencies {
    implementation(project(":libs:beregning"))
    implementation(project(":libs:oppdrag"))
    testImplementation(project(":libs:beregningsflyt-verifikasjon"))
    testImplementation(testFixtures(project(":libs:beregning")))
}

tasks.withType<Test>().configureEach {
    dependsOn(":libs:beregning:processResources")
    classpath = files(project(":libs:beregning").layout.buildDirectory.dir("resources/main")) + classpath
    listOf("kotest.filter.tests", "kotest.tags", "kotest.tags.include", "kotest.tags.exclude").forEach { key ->
        System.getProperty(key)?.takeIf { it.isNotBlank() }?.let { value ->
            systemProperty(key, value)
        }
    }
}
