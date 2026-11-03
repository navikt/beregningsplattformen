plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:6.2.6")
    testImplementation("io.kotest:kotest-assertions-core:6.2.6")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("publiserBibliotek") {
            id = "publiser-bibliotek"
            implementationClass = "PubliserBibliotekPlugin"
        }
        register("påvirkedeModuler") {
            id = "paavirkede-moduler"
            implementationClass = "PåvirkedeModulerPlugin"
        }
    }
}
