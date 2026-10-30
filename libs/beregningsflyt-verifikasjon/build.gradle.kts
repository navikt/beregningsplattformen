import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("no.nav.ktlintgradle.ktlint")
    `java-library`
    `maven-publish`
}

group = "no.nav.utbetaling.beregningsplattformen"
version = providers.gradleProperty("publicationVersion").getOrElse("0.0.0-SNAPSHOT")

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "beregningsflyt-verifikasjon"
        }
    }

    repositories {
        providers.environmentVariable("MAVEN_PUBLISH_URL").orNull?.let { repositoryUrl ->
            maven {
                name = "GitHubPackages"
                url = uri(repositoryUrl)
                credentials {
                    username = providers.environmentVariable("MAVEN_PUBLISH_USER").get()
                    password = providers.environmentVariable("MAVEN_PUBLISH_TOKEN").get()
                }
            }
        }
    }
}

tasks.named<Test>("test") {
    dependsOn(tasks.named("jar"))
}
