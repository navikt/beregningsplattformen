plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val jacksonVersion = "3.2.4"

dependencies {
    // TODO: Bør vi bruke java-library og api her, siden Oppdragstidslinje eksponerer Tidslinjesamling, Tidslinje og Periode fra tid?
    implementation(project(":libs:tid"))

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("tools.jackson.module:jackson-module-jakarta-xmlbind-annotations:$jacksonVersion")

    testImplementation(testFixtures(project(":libs:tid")))
}
