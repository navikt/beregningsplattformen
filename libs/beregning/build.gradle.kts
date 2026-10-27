import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.kover)
}

group = "no.nav.sokos"

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.trekkrutine2025)
    api(libs.trekkrutine2026)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.logging)
    implementation(kotlin("reflect"))

    testFixturesApi(libs.bundles.kotest)
    testFixturesImplementation(libs.bundles.jackson)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.logback.classic)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-Xannotation-default-target=param-property",
        )
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
    classpath = files(
        sourceSets.main
            .get()
            .output.resourcesDir,
    ) + classpath
    useJUnitPlatform()
    listOf("kotest.filter.tests", "kotest.tags", "kotest.tags.include", "kotest.tags.exclude").forEach { key ->
        System.getProperty(key)?.takeIf { it.isNotBlank() }?.let { value ->
            systemProperty(key, value)
        }
    }
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = FULL
        showStandardStreams = true
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
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
