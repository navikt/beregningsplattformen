// Avhengigheter felles for alle moduler er definert i rot build.gradle.kts
plugins {
    `java-test-fixtures`
}

sourceSets {
    testFixtures {
        kotlin.srcDir("testFixtures")
        resources.srcDir("testFixtures")
    }
}
