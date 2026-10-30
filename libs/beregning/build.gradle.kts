import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("no.nav.ktlintgradle.ktlint")
    alias(libs.plugins.kover)
}

group = "no.nav.utbetaling.beregningsplattformen"
version = providers.gradleProperty("publicationVersion").getOrElse("0.0.0-SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "beregning"
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

dependencies {
    implementation(project(":libs:oppdrag"))
    api(libs.kotlinx.serialization.json)
    implementation(libs.trekkrutine2025)
    api(libs.trekkrutine2026)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.logging)
    implementation(kotlin("reflect"))

    testImplementation(project(":libs:beregningsflyt-verifikasjon"))
    testFixturesApi(libs.kotest.assertions.core)
    testFixturesImplementation(libs.bundles.jackson)
    testRuntimeOnly(libs.logback.classic)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

if (System.getenv("CI") == null) {
    kover {
        disable()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("ktlintFormat")
}

tasks.withType<Test>().configureEach {
    // Integration fixtures and compiled timeline guards need directories, not entries in the main JAR.
    classpath = files(
        sourceSets.main
            .get()
            .output,
    ) + classpath
    listOf("kotest.filter.tests", "kotest.tags", "kotest.tags.include", "kotest.tags.exclude").forEach { key ->
        System.getProperty(key)?.takeIf { it.isNotBlank() }?.let { value ->
            systemProperty(key, value)
        }
    }
    testLogging {
        showStandardStreams = true
    }
    val profileMode =
        providers
            .gradleProperty("profileMode")
            .orNull
            ?.trim()
            ?.lowercase()
    when (profileMode) {
        null, "", "default" -> Unit
        "no-tiered" -> jvmArgs("-XX:-TieredCompilation")
        "no-inline" -> jvmArgs("-XX:MaxInlineLevel=0")
        "xint" -> jvmArgs("-Xint")
        else -> throw IllegalArgumentException("Unknown -PprofileMode=$profileMode. Expected default, no-tiered, no-inline, xint")
    }
    if (providers.gradleProperty("testJfr").orNull?.toBooleanStrictOrNull() == true) {
        jvmArgs(
            "-XX:FlightRecorderOptions=stackdepth=256",
            "-XX:StartFlightRecording=filename=build/reports/test.jfr,dumponexit=true,settings=profile,jdk.ExecutionSample#period=2ms,jdk.NativeMethodSample#period=2ms",
        )
    }
    finalizedBy("koverHtmlReport")
}
