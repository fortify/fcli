plugins { id("fcli.java-conventions") }

import java.time.format.DateTimeFormatter

val fcliActionSchemaVersion = property("fcliActionSchemaVersion") as String
val buildTime = rootProject.extra["buildTime"] as java.time.LocalDateTime

// Generate build properties (incremental & avoiding unnecessary rewrites)
val generateFcliBuildProperties = tasks.register("generateFcliBuildProperties") {
    group = "build resources"
    description = "Generate fcli build properties and native-image resource-config"
    val outputDirProvider = layout.buildDirectory.dir("generated-build-properties/com/fortify/cli/common")
    val resourceConfigOutputDirProvider = layout.buildDirectory.dir("generated-build-properties/META-INF/native-image/fcli-build-properties")
    inputs.property("projectVersion", project.version)
    inputs.property("fcliActionSchemaVersion", fcliActionSchemaVersion)
    inputs.property("buildTime", buildTime.toString())
    outputs.dir(outputDirProvider)
    outputs.dir(resourceConfigOutputDirProvider)
    doLast {
        val outputDir = outputDirProvider.get().asFile.apply { mkdirs() }
        val propsFile = outputDir.resolve("fcli-build.properties")
        val propsContent = buildString {
            appendLine("projectName=fcli")
            appendLine("projectVersion=${project.version}")
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            appendLine("buildDate=${buildTime.format(fmt)}")
            appendLine("actionSchemaVersion=$fcliActionSchemaVersion")
        }
        if (!propsFile.exists() || propsFile.readText() != propsContent) {
            propsFile.writeText(propsContent)
        }
        val resourceConfigDir = resourceConfigOutputDirProvider.get().asFile.apply { mkdirs() }
        val rcFile = resourceConfigDir.resolve("resource-config.json")
        val rcContent = "{\"resources\":[\n  {\"pattern\":\"com/fortify/cli/common/fcli-build.properties\"}\n]}\n"
        if (!rcFile.exists() || rcFile.readText() != rcContent) {
            rcFile.writeText(rcContent)
        }
    }
}

// Add generated dir to main output
extensions.configure<org.gradle.api.tasks.SourceSetContainer>("sourceSets") {
    named("main") {
        output.dir(mapOf("builtBy" to generateFcliBuildProperties.name), layout.buildDirectory.dir("generated-build-properties").get())
    }
}