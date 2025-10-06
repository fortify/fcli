package com.fortify.fcli.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.*
import java.io.File
import org.gradle.api.tasks.JavaExec
import org.asciidoctor.gradle.jvm.AsciidoctorTask

class FcliJavaConventionsPlugin: Plugin<Project> {
    override fun apply(project: Project) = project.run {
        plugins.apply("java")
        plugins.apply("io.freefair.lombok")
        plugins.apply("com.github.ben-manes.versions")

        extensions.configure<org.gradle.api.plugins.JavaPluginExtension>("java") {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        // Exclude zip resources from normal resources set
        extensions.configure<SourceSetContainer>("sourceSets") {
            named("main") {
                resources { exclude("**/zip/*", "**/zip") }
            }
        }

        // Dependencies leveraging BOM project if present
        afterEvaluate {
            val bomRef = findProperty("fcliBomRef") as String?
            if (bomRef != null) {
                dependencies.apply {
                    add("implementation", platform(project(bomRef)))
                    add("annotationProcessor", platform(project(bomRef)))
                }
            }
            dependencies.apply {
                // Exclude slf4j transitive dependency for io.modelcontextprotocol
                add("implementation", "io.modelcontextprotocol.sdk:mcp-core") {
                    exclude(group = "org.slf4j", module = "slf4j-api")
                }
                add("implementation", "io.modelcontextprotocol.sdk:mcp-json-jackson2") {
                    exclude(group = "org.slf4j", module = "slf4j-api")
                }
                add("annotationProcessor", "info.picocli:picocli-codegen")
                add("compileOnly", "com.formkiq:graalvm-annotations")
                add("annotationProcessor", "com.formkiq:graalvm-annotations-processor")
                add("implementation", "com.konghq:unirest-java")
                add("implementation", "com.konghq:unirest-objectmapper-jackson")
                add("implementation", "org.springframework:spring-expression")
                add("implementation", "org.springframework:spring-context")
                add("compileOnly", "com.google.code.findbugs:jsr305")
                add("implementation", "org.slf4j:slf4j-api")
                add("implementation", "org.slf4j:jcl-over-slf4j")
                add("implementation", "ch.qos.logback:logback-classic")
                add("implementation", "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
                add("implementation", "com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
                add("implementation", "com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
                add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
                add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
                add("implementation", "com.github.freva:ascii-table")
                add("implementation", "org.jasypt:jasypt")
                add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
                add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
                add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
                add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
                add("implementation", "org.apache.commons:commons-lang3:3.18.0")
                add("implementation", "org.apache.commons:commons-compress")
                add("implementation", "org.jsoup:jsoup")
            }
        }

        tasks.withType(org.gradle.api.tasks.compile.JavaCompile::class).configureEach {
            if (name == "compileJava") {
                options.compilerArgs.addAll(listOf("-Averbose=true", "-Adisable.reflect.config=true"))
            }
        }

        tasks.withType(org.gradle.api.tasks.testing.Test::class).configureEach {
            useJUnitPlatform()
            testLogging { events = setOf(TestLogEvent.FAILED) }
        }

        // Directories for generated resources
        val generatedZipResourcesDir = layout.buildDirectory.dir("generated-zip-resources")
        val generatedActionOutputResourcesDir = layout.buildDirectory.dir("generated-action-output-resources")
        val generatedResourceConfigDir = layout.buildDirectory.dir("generated-resource-config")
        // Ensure directories exist already during configuration so input validation passes
        generatedZipResourcesDir.get().asFile.mkdirs()
        generatedActionOutputResourcesDir.get().asFile.mkdirs()
        generatedResourceConfigDir.get().asFile.mkdirs()

        // Task to ensure directories exist to satisfy Gradle input validation
        val ensureGeneratedDirs = tasks.register("ensureGeneratedDirs") {
            doFirst {
                generatedZipResourcesDir.get().asFile.mkdirs()
                generatedActionOutputResourcesDir.get().asFile.mkdirs()
            }
        }

        // Aggregator tasks
        val generateZipResources = tasks.register("generateZipResources")
        val buildTimeActions = tasks.register("buildTimeActions")

        // Auto-register any task named buildTimeAction_* as dependency of buildTimeActions
        tasks.whenTaskAdded {
            if (name.startsWith("buildTimeAction_")) {
                buildTimeActions.configure { dependsOn(this@whenTaskAdded) }
            }
        }

        // Internal reusable registration function used by auto-discovery & legacy manual registration
        fun registerActionZipTaskInternal(taskName: String, srcRel: String, destRel: String, archive: String = "actions.zip", description: String? = null) {
            val schemaVersion = (findProperty("fcliActionSchemaVersion") ?: "").toString()
            val srcDir = layout.projectDirectory.dir(srcRel)
            if (!srcDir.asFile.exists()) return
            val zipTask = tasks.register<Zip>(taskName) {
                group = "build resources"
                this.description = description ?: "Package action yaml definitions ($srcRel) into $archive"
                from(srcDir) {
                    include("*.yaml")
                    filter { line: String ->
                        if (project.version.toString().startsWith("0.")) line else line.replace(
                            Regex("https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev.*.json"),
                            "https://fortify.github.io/fcli/schemas/action/fcli-action-schema-$schemaVersion.json"
                        )
                    }
                }
                destinationDirectory.set(layout.buildDirectory.dir("generated-zip-resources/$destRel"))
                archiveFileName.set(archive)
                inputs.dir(srcDir)
                inputs.property("projectVersion", project.version)
                inputs.property("fcliActionSchemaVersion", schemaVersion)
                outputs.file(layout.buildDirectory.file("generated-zip-resources/$destRel/$archive"))
            }
            tasks.named("generateZipResources").configure { dependsOn(zipTask) }
        }

        // Expose legacy helper (still used by older module scripts if any remain)
        project.extensions.extraProperties.set("registerActionZipTask", { cfg: Map<String, Any?> ->
            registerActionZipTaskInternal(
                taskName = cfg["name"] as String,
                srcRel = cfg["src"] as String,
                destRel = cfg["dest"] as String,
                archive = (cfg["archive"] as? String) ?: "actions.zip",
                description = cfg["description"] as? String
            )
        })

        // Auto-discover action definition directories: src/main/resources/com/fortify/cli/<moduleName>/actions/zip
        run {
            val cliRoot = project.file("src/main/resources/com/fortify/cli")
            if (cliRoot.isDirectory) {
                val discovered = cliRoot.listFiles()?.filter { dir ->
                    dir.isDirectory && project.file("${dir.path}/actions/zip").isDirectory
                }?.sortedBy { it.name } ?: emptyList()
                discovered.forEach { moduleDir ->
                    val name = moduleDir.name
                    val srcRel = "src/main/resources/com/fortify/cli/${name}/actions/zip"
                    val destRel = "com/fortify/cli/${name}"
                    val defaultTaskName = if (discovered.size == 1) "zipResources_actions" else "zipResources_${name}" // avoid clashes if multiple
                    // Only register if there is at least one yaml file to include
                    val hasYaml = project.fileTree(project.file(srcRel)) { include("*.yaml") }.files.any()
                    if (hasYaml && tasks.names.none { it == defaultTaskName }) {
                        registerActionZipTaskInternal(defaultTaskName, srcRel, destRel)
                    }
                }
            }
        }

        // Resource-config generation incremental
        tasks.register("generateResourceConfig") {
            group = "build resources"
            description = "Generate GraalVM resource-config.json"
            dependsOn(ensureGeneratedDirs)
            dependsOn(generateZipResources, buildTimeActions)
            inputs.dir("src/main/resources")
            inputs.dir(generatedZipResourcesDir)
            inputs.dir(generatedActionOutputResourcesDir)
            val outputDirProvider = generatedResourceConfigDir.map { it.dir("META-INF/native-image/fcli-generated/${project.name}") }
            outputs.dir(outputDirProvider)
            doLast {
                val entries = mutableListOf<String>()
                // Helper to add files from a base directory
                fun addFiles(baseDir: java.io.File, tree: org.gradle.api.file.FileTree) {
                    tree.files.filter { it.isFile }.sorted().forEach { f ->
                        val rel = f.relativeTo(baseDir).invariantSeparatorsPath
                        entries += "\n  {\"pattern\":\"$rel\"}"
                    }
                }
                val srcBase = project.layout.projectDirectory.dir("src/main/resources").asFile
                addFiles(srcBase, project.fileTree(srcBase) { exclude("**/i18n/**", "META-INF/**", "**/zip/**") })
                val genZipBase = generatedZipResourcesDir.get().asFile
                addFiles(genZipBase, project.fileTree(genZipBase))
                val genActionBase = generatedActionOutputResourcesDir.get().asFile
                addFiles(genActionBase, project.fileTree(genActionBase))
                if (entries.isNotEmpty()) {
                    val contents = "{" + "\"resources\":[" + entries.joinToString(",") + "\n]}"
                    val outputDir = outputDirProvider.get().asFile.apply { mkdirs() }
                    val outFile = File(outputDir, "resource-config.json")
                    if (!outFile.exists() || outFile.readText() != contents) {
                        outFile.writeText(contents)
                    }
                }
            }
        }
        // Add generated dirs to main output
        extensions.configure<SourceSetContainer>("sourceSets") {
            named("main") {
                output.dir(generatedZipResourcesDir, "builtBy" to generateZipResources.getName())
                output.dir(generatedActionOutputResourcesDir, "builtBy" to buildTimeActions.getName())
                output.dir(generatedResourceConfigDir, "builtBy" to "generateResourceConfig")
            }
        }

        tasks.withType(JavaExec::class).configureEach {
            fun ensureAddOpens(target: String) {
                val current = this.jvmArgs ?: emptyList()
                if (!current.contains(target)) {
                    jvmArgs("--add-opens", target)
                }
            }
            ensureAddOpens("java.base/java.io=ALL-UNNAMED")
            ensureAddOpens("java.base/sun.nio.ch=ALL-UNNAMED")
        }
        // Apply same opens to Asciidoctor JVM (using deprecated forkOptions with suppression until new API exposed)
        tasks.withType(AsciidoctorTask::class).configureEach {
            @Suppress("DEPRECATION")
            forkOptions {
                val needed = listOf(
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
                )
                val current = jvmArgs ?: emptyList()
                needed.filter { n -> !current.contains(n) }.forEach { jvmArgs(it) }
            }
        }
    }
}