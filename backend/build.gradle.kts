import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("com.gradleup.shadow") version "9.2.2"
}

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.3.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.3.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.2")
    implementation("org.xerial:sqlite-jdbc:3.51.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.fidelius.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.named<ProcessResources>("processResources") {
    val frontendStatic = File(project.projectDir, "../frontend/static")
    if (frontendStatic.exists()) {
        from(frontendStatic) {
            into("static")
        }
    } else {
        logger.lifecycle("Frontend static folder not found at: ${frontendStatic.absolutePath} — skipping copy into resources.")
    }
}