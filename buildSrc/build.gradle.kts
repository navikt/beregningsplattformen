plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("publiserBibliotek") {
            id = "publiser-bibliotek"
            implementationClass = "PubliserBibliotekPlugin"
        }
    }
}
