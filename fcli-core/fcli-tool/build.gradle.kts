plugins {
    id("fcli.module-conventions")
    id("de.undercouch.download")
}

// Generate tool definitions & resource-config entry
val toolDefinitionsSource = "https://github.com/fortify/tool-definitions/releases/download/v1/tool-definitions.yaml.zip"
val toolDefinitionsFile = "tool-definitions.yaml.zip"
val relDir = "com/fortify/cli/tool/config"
val downloadOutputDir = layout.buildDirectory.dir("tool-definitions/$relDir")
val resourceConfigOutputDir = layout.buildDirectory.dir("tool-definitions/META-INF/native-image/tool-definitions")
val downloadedFile = downloadOutputDir.map { it.file(toolDefinitionsFile) }

// Correct typed registration
val downloadToolDefinitions = tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadToolDefinitions") {
    group = "build resources"
    description = "Download tool-definitions archive (only if modified)"
    src(toolDefinitionsSource)
    dest(downloadedFile.get().asFile)
    onlyIfModified(true)
    useETag("all")
    outputs.file(downloadedFile)
    doFirst { downloadOutputDir.get().asFile.mkdirs() }
}

val generateToolDefinitionResources = tasks.register("generateToolDefinitionResources") {
    group = "build resources"
    description = "Generate native-image resource-config for tool definitions"
    dependsOn(downloadToolDefinitions)
    inputs.file(downloadedFile)
    outputs.file(resourceConfigOutputDir.map { it.file("resource-config.json") })
    doLast {
        val resourceConfigDirFile = resourceConfigOutputDir.get().asFile.apply { mkdirs() }
        val relFile = "$relDir/$toolDefinitionsFile"
        val resourceConfigContents = "{\"resources\":[{\"pattern\":\"$relFile\"}]}"
        val rcFile = resourceConfigDirFile.resolve("resource-config.json")
        if (!rcFile.exists() || rcFile.readText() != resourceConfigContents) {
            rcFile.writeText(resourceConfigContents)
        }
    }
}

// Add to runtime resources
sourceSets.named("main") {
    output.dir(mapOf("builtBy" to generateToolDefinitionResources.name), layout.buildDirectory.dir("tool-definitions"))
}