plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.google.cloud.tools.jib") version "3.1.4"
}

group = "net.perfectdreams.floppapower"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    maven("https://repo.perfectdreams.net/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("net.dv8tion:JDA:5.0.0-beta.18")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    api("io.github.microutils:kotlin-logging:2.0.6")
    api("ch.qos.logback:logback-classic:1.4.14")
}

jib {
    to {
        image = "ghcr.io/lorittabot/floppapower"

        auth {
            username = System.getProperty("DOCKER_USERNAME") ?: System.getenv("DOCKER_USERNAME")
            password = System.getProperty("DOCKER_PASSWORD") ?: System.getenv("DOCKER_PASSWORD")
        }
    }

    from {
        image = "eclipse-temurin:17-jdk-alpine"
    }
}