plugins {
    id("fcli.module-conventions")
    id("de.undercouch.download")
}

// Generate tool definitions & resource-config entry
val generateToolDefinitionResources = tasks.register("generateToolDefinitionResources") {
    group = "build resources"
    description = "Download tool-definitions archive and generate native-image resource-config"
    val toolDefinitionsSource = "https://github.com/fortify/tool-definitions/releases/download/v1/tool-definitions.yaml.zip"
    val toolDefinitionsFile = "tool-definitions.yaml.zip"
    val relDir = "com/fortify/cli/tool/config"
    val downloadOutputDir = layout.buildDirectory.dir("tool-definitions/$relDir")
    val resourceConfigOutputDir = layout.buildDirectory.dir("tool-definitions/META-INF/native-image/tool-definitions")
    inputs.property("toolDefinitionsSource", toolDefinitionsSource)
    val downloaded = downloadOutputDir.map { it.file(toolDefinitionsFile) }
    outputs.file(downloaded)
    outputs.file(resourceConfigOutputDir.map { it.file("resource-config.json") })
    doLast {
        val downloadDirFile = downloadOutputDir.get().asFile.apply { mkdirs() }
        val resourceConfigDirFile = resourceConfigOutputDir.get().asFile.apply { mkdirs() }
        de.undercouch.gradle.tasks.download.DownloadAction(project).apply {
            src(toolDefinitionsSource)
            dest(downloadDirFile.resolve(toolDefinitionsFile))
            onlyIfModified(true)
            useETag("all")
            execute()
        }
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
