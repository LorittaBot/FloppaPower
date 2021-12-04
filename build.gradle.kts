plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
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
    implementation("net.dv8tion:JDA:5.0.0-alpha.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.0")
    implementation("org.jetbrains.exposed:exposed-core:0.31.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.31.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.31.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.31.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("org.postgresql:postgresql:42.2.24")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("io.ktor:ktor-server-netty:1.6.5")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")

    // Kord
    // implementation("dev.kord:kord-gateway:0.8.x-SNAPSHOT")
    // implementation("dev.kord:kord-rest:0.8.x-SNAPSHOT")

    // Discord InteraKTions
    // implementation("net.perfectdreams.discordinteraktions:gateway-kord:0.0.10-SNAPSHOT")

    api("io.github.microutils:kotlin-logging:2.0.6")
    // Async Appender is broken in alpha5
    // https://stackoverflow.com/questions/58742485/logback-error-no-attached-appenders-found
    api("ch.qos.logback:logback-classic:1.3.0-alpha4")
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

tasks {
    val runnableJar = runnableJarTask(
        DEFAULT_SHADED_WITHIN_JAR_LIBRARIES,
        configurations.runtimeClasspath.get(),
        jar.get(),
        "net.perfectdreams.floppapower.FloppaPowerLauncher",
        mapOf()
    )

    "build" {
        // This should be ran BEFORE the JAR is compiled!
        dependsOn(runnableJar)
    }
}