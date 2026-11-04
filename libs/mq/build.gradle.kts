plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api(project(":libs:log"))
    api("com.ibm.mq:com.ibm.mq.jakarta.client:10.0.1.0")
    api("io.opentelemetry:opentelemetry-api:1.67.0")
    api("io.opentelemetry:opentelemetry-context:1.67.0")
}
