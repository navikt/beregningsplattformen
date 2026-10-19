plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

dependencies {
    api("ch.qos.logback:logback-classic:1.6.5")
}
