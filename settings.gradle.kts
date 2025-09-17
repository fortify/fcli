import java.util.Properties

rootProject.name = "fcli"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("io.freefair.lombok") version "8.13"
        id("com.github.ben-manes.versions") version "0.52.0"
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("com.github.jk1.dependency-license-report") version "2.9"
        id("org.asciidoctor.jvm.convert") version "4.0.4"
        id("de.undercouch.download") version "5.6.0"
        id("com.google.protobuf") version "0.9.4"
    }
    includeBuild("build-logic")
}

// Load gradle.properties so we can dynamically include projects based on *Ref entries
val props = Properties().apply { file("gradle.properties").inputStream().use { load(it) } }
val refPatterns = props.getProperty("refPatterns").split(',').map { it.trim().toRegex() }
props.stringPropertyNames()
    .filter { key -> refPatterns.any { it.matches(key) } }
    .forEach { key ->
        val path = props.getProperty(key)
        if (!path.isNullOrBlank()) {
            // Resolve physical directory for project path
            val dirPath = path.trimStart(':').replace(':','/')
            val dir = file(dirPath)
            if (dir.exists()) {
                include(path)
            } else {
                println("[settings] Skipping include for $path (directory $dirPath missing)")
            }
        }
    }