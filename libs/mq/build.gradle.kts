plugins {
    kotlin("jvm")
    id("no.nav.ktlintgradle.ktlint")
    `java-library`
}

dependencies {
    api(project(":libs:log"))
    api("com.ibm.mq:com.ibm.mq.jakarta.client:10.0.0.5")
    api("io.opentelemetry:opentelemetry-api:1.65.0")
    api("io.opentelemetry:opentelemetry-context:1.65.0")
}
