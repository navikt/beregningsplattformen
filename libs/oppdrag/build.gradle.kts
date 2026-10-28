plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val jacksonVersion = "3.2.4"

dependencies {
    api(project(":libs:tid"))

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("tools.jackson.module:jackson-module-jakarta-xmlbind-annotations:$jacksonVersion")

    testImplementation(testFixtures(project(":libs:tid")))
}
