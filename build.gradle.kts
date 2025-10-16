import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Year

plugins {
    id("com.github.jk1.dependency-license-report") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("org.asciidoctor.jvm.convert") apply false
    id("io.freefair.lombok") apply false
    id("com.github.ben-manes.versions") apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
}

group = "com.fortify.cli"

val buildTime: LocalDateTime = LocalDateTime.now()
val currentYear: Int = Year.now().value
val copyrightYears: String = if (currentYear == 2021) "2021" else "2021-$currentYear"
fun computeVersion(): String {
    val v = findProperty("version") as String?
    return if (v.isNullOrBlank() || v == "unspecified") {
        buildTime.format(DateTimeFormatter.ofPattern("0.yyyyMMdd.HHmmss"))
    } else v
}
version = computeVersion()

extra["buildTime"] = buildTime
extra["fcliActionSchemaUrl"] = "https://fortify.github.io/fcli/schemas/action/fcli-action-schema-${property("fcliActionSchemaVersion")}.json"

allprojects {
    // Eclipse metadata (kept for parity with old build)
    pluginManager.apply("eclipse")
    version = rootProject.version
    val distDir = layout.buildDirectory.dir("dist")
    val releaseAssetsDir = distDir.map { it.dir("release-assets") }
    extra["distDir"] = distDir.get().asFile.absolutePath
    extra["releaseAssetsDir"] = releaseAssetsDir.get().asFile.absolutePath
    extra["gradleHelpersLocation"] = "https://raw.githubusercontent.com/fortify/shared-gradle-helpers/1.8"

    // Derive *RefDir properties for every *Ref matching refPatterns
    val refPatterns = (property("refPatterns") as String).split(',').map { it.trim().toRegex() }
    properties.forEach { (k, v) ->
        if (refPatterns.any { it.matches(k) }) {
            extra["${k}Dir"] = "$rootDir" + (v as String).replace(":", "/")
        }
    }
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
    // Apply Spotless only where Java plugin is present; target common json.record & output packages
    pluginManager.withPlugin("java") {
        apply(plugin = "com.diffplug.spotless")
        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension>("spotless") {
            java {
                // Limit to com.fortify Java sources only; exclude generated/compiled dirs
                target("**/src/**/java/com/fortify/**/*.java")
                targetExclude("**/build/**", "**/bin/**", "**/generated-sources/**", "**/generated-test-sources/**")
                // Minimal normalization per request: only ensure standard license header; no other formatting tweaks
                // Re-introduced: import cleanup (order + unused removal) as requested
                removeUnusedImports()
                importOrder("java", "javax", "org", "com", "")
                // Step 1: Replace any tabs with 4 spaces (preserving visual width)
                custom("tabsToSpaces") { content: String ->
                    if (!content.contains('\t')) content else content.replace("\t", "    ")
                }
                // Step 2: Ensure indentation uses multiples of 4 spaces for code lines (skip Javadoc/ block comment continuations and blank lines)
                custom("normalizeIndentation") { content: String ->
                    val lines: List<String> = content.split("\n")
                    val normalized = lines.map { line: String ->
                        if (line.isEmpty()) return@map line
                        val leading = line.takeWhile { ch -> ch == ' ' }
                        val rest = line.drop(leading.length)
                        if (rest.startsWith("*") || rest.startsWith("/*") || rest.startsWith("*/")) return@map line
                        if (leading.isEmpty()) return@map line
                        val spaceCount = leading.length
                        val adjustedCount = if (spaceCount % 4 == 0) spaceCount else (spaceCount / 4) * 4
                        val newLeading = " ".repeat(adjustedCount)
                        if (newLeading == leading) line else newLeading + rest
                    }
                    normalized.joinToString("\n")
                }
                custom("stripOldOpenTextHeader") { content: String ->
                    val pattern = Regex("""^/\*{1,}.*?Open Text.*?\*/\s*""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    pattern.replace(content) { mr -> if (mr.value.contains("warranties", ignoreCase = true)) "" else mr.value }
                }
                // Conditionally ensure standard license header; skip JAXB-generated sources
                val stdHeader = """/*
 * Copyright $copyrightYears Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
"""
                custom("conditionalLicenseHeader") { content: String ->
                    // Detect JAXB or other generated markers near the top of the file; if found, leave unchanged
                    val firstLines = content.lineSequence().take(15).joinToString("\n")
                    val generatedMarkers = listOf(
                        Regex("^//\\s*This file was generated by the Eclipse Implementation of JAXB", RegexOption.MULTILINE),
                        Regex("^//\\s*Generated on:", RegexOption.MULTILINE)
                    )
                    if (generatedMarkers.any { it.containsMatchIn(firstLines) }) return@custom content
                    // If standard header already present (allow year drift), keep as-is
                    val hasHeader = Regex("^/\\*\\s*Copyright ", RegexOption.MULTILINE).containsMatchIn(firstLines)
                    if (hasHeader) return@custom content
                    // Insert header before first package declaration (or at start if none)
                    val pkgIndex = content.indexOf("\npackage ").takeIf { it >= 0 } ?: content.indexOf("package ")
                    return@custom when {
                        pkgIndex >= 0 -> stdHeader + content
                        else -> stdHeader + content // fallback; typically there is a package line
                    }
                }
            }
        }
        // Optionally auto-apply formatting when -PautoFormat=true (or property set in gradle.properties)
        val autoFormat = (findProperty("autoFormat") as String?)?.toBoolean() == true
        if (autoFormat) {
            // Ensure spotlessApply runs before any Java compilation tasks in this project
            tasks.matching { it.name == "spotlessApply" }.configureEach {
                // no-op placeholder for potential logging
            }
            tasks.withType<JavaCompile>().configureEach { dependsOn("spotlessApply") }
        }
    }
    tasks.register("createDistDir") {
        doFirst {
            distDir.get().asFile.mkdirs()
            releaseAssetsDir.get().asFile.mkdirs()
        }
    }
}

// Root tasks mirroring previous Groovy implementation

val rawFcliAppRef = property("fcliAppRef") as String
val fcliAppPath = if (rawFcliAppRef.startsWith(":")) rawFcliAppRef else ":$rawFcliAppRef"
val fcliAppRefDir = extra["${"fcliAppRef"}Dir"] as String? ?: "$rootDir/${rawFcliAppRef.trimStart(':').replace(':','/')}"
val rawFcliFunctionalTestRef = property("fcliFunctionalTestRef") as String
val fcliFunctionalTestPath = if (rawFcliFunctionalTestRef.startsWith(":")) rawFcliFunctionalTestRef else ":$rawFcliFunctionalTestRef"

tasks.register<Delete>("clean") { delete("build") }

// Removed custom build task that shadowed lifecycle build.
// Provide explicit task to collect application jar if needed.
tasks.register<Copy>("collectAppJar") {
    dependsOn("$fcliAppPath:build")
    from("$fcliAppRefDir/build/libs/fcli.jar")
    into("build/libs")
}

tasks.register<Copy>("dist") {
    dependsOn("createDistDir", "distFcliCompletion")
    from(projectDir) { include("LICENSE.txt") }
    into(layout.buildDirectory.dir("dist/release-assets"))
}

tasks.register("distThirdPartyReleaseAsset") { dependsOn("$fcliAppPath:distThirdPartyReleaseAsset") }

tasks.register("distFtest") { dependsOn("$fcliFunctionalTestPath:distFtest") }

tasks.register<Copy>("distFcliCompletion") {
    group = "distribution"
    description = "Copy fcli_completion to dist directory"
    dependsOn(":fcli-other:fcli-autocomplete:dist")
    val srcFile = providers.provider { file("fcli-other/fcli-autocomplete/build/dist/fcli_completion") }
    from(srcFile)
    into(layout.buildDirectory.dir("dist"))
    // Declare inputs/outputs for up-to-date checking
    inputs.file(srcFile)
    outputs.file(layout.buildDirectory.file("dist/fcli_completion"))
    doFirst {
        if (!srcFile.get().exists()) {
            throw GradleException("Expected autocomplete script not found: ${'$'}{srcFile.get()} - ensure :fcli-other:fcli-autocomplete:dist ran successfully")
        }
    }
}

tasks.register("distAll") {
    group = "distribution"
    description = "Aggregate all distribution artifacts"
    dependsOn(
        "dist",
        "distThirdPartyReleaseAsset",
        "distFtest",
        "distFcliCompletion",
        ":fcli-other:fcli-doc:dist"
    )
}

// Aggregate root build task (root project has no Java/Base plugin applied)
tasks.register("build") {
    group = "build"
    description = "Aggregate build for all subprojects with a build task and copy fcli.jar to build/libs"
    // Collect only subprojects that actually have a 'build' task (skip synthetic container projects like :fcli-core)
    val buildTaskPaths = subprojects.mapNotNull { sp -> sp.tasks.findByName("build")?.path }
    dependsOn(buildTaskPaths)
    dependsOn("collectAppJar")
}