plugins { id("fcli.java-conventions") }

import java.time.format.DateTimeFormatter

val fcliActionSchemaVersion = property("fcliActionSchemaVersion") as String
val buildTime = rootProject.extra["buildTime"] as java.time.LocalDateTime

// Zip task previously in Groovy file
val zipResourcesTemplates = tasks.register<Zip>("zipResources_templates") {
    group = "build resources"
    description = "Package common action template yaml definitions into a zip"
    val srcDir = layout.projectDirectory.dir("src/main/resources/com/fortify/cli/common/actions/zip")
    from(srcDir) {
        include("*.yaml")
        filter { line: String ->
            if (project.version.toString().startsWith("0.")) line else line.replace(
                Regex("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev.*.json"),
                "https://fortify.github.io/fcli/schemas/action/fcli-action-schema-$fcliActionSchemaVersion.json"
            )
        }
    }
    destinationDirectory.set(layout.buildDirectory.dir("generated-zip-resources/com/fortify/cli/common"))
    archiveFileName.set("actions.zip")
    inputs.dir(srcDir)
    inputs.property("projectVersion", project.version)
    inputs.property("fcliActionSchemaVersion", fcliActionSchemaVersion)
    outputs.file(layout.buildDirectory.file("generated-zip-resources/com/fortify/cli/common/actions.zip"))
}

// Ensure aggregator depends on this zip task for resource-config consistency
tasks.named("generateZipResources").configure { dependsOn(zipResourcesTemplates) }

// Register with helper from conventions plugin
@Suppress("UNCHECKED_CAST")
val registerActionZipTask = project.extra["registerActionZipTask"] as (Map<String, Any?>) -> Unit
// Already defined custom zip task above; no need to register via helper

// Generate build properties (incremental & avoiding unnecessary rewrites)
val generateFcliBuildProperties = tasks.register("generateFcliBuildProperties") {
    group = "build resources"
    description = "Generate fcli build properties and native-image resource-config"
    val outputDirProvider = layout.buildDirectory.dir("generated-build-properties/com/fortify/cli/common")
    val resourceConfigOutputDirProvider = layout.buildDirectory.dir("generated-build-properties/META-INF/native-image/fcli-build-properties")
    // Capture providers to avoid querying project in task action
    val projectVersionProvider = providers.provider { project.version.toString() }
    inputs.property("projectVersion", projectVersionProvider)
    inputs.property("fcliActionSchemaVersion", fcliActionSchemaVersion)
    inputs.property("buildTime", buildTime.toString())
    outputs.dir(outputDirProvider)
    outputs.dir(resourceConfigOutputDirProvider)
    doLast {
        val outputDir = outputDirProvider.get().asFile.apply { mkdirs() }
        val propsFile = outputDir.resolve("fcli-build.properties")
        val propsContent = buildString {
            appendLine("projectName=fcli")
            appendLine("projectVersion=${projectVersionProvider.get()}")
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