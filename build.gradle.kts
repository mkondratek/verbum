plugins {
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    kotlin("jvm") version "2.2.0"
}

group = "com.mkondratek.verbum"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
