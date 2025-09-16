import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.github.jk1.dependency-license-report") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("org.asciidoctor.jvm.convert") apply false
    id("io.freefair.lombok") apply false
    id("com.github.ben-manes.versions") apply false
}

group = "com.fortify.cli"

val buildTime: LocalDateTime = LocalDateTime.now()
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