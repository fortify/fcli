import java.io.ByteArrayOutputStream
import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    id("fcli.java-conventions")
    id("org.asciidoctor.jvm.convert")
}

dependencies {
    val commonRef = project.findProperty("fcliCommonRef") as String
    val appRef = project.findProperty("fcliAppRef") as String
    implementation(project(commonRef))
    implementation(project(appRef))
    runtimeOnly("info.picocli:picocli-codegen")
    implementation("com.github.victools:jsonschema-generator:4.38.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.38.0")
}

val docsSrcDir = layout.projectDirectory.dir("src/docs")
val staticAsciiDocSrcDir = docsSrcDir.dir("asciidoc/static")
val versionedAsciiDocSrcDir = docsSrcDir.dir("asciidoc/versioned")
val asciiDocTemplatesSrcDir = docsSrcDir.dir("asciidoc/templates")
val docsOutDir = layout.buildDirectory.dir("generated-docs")
val asciiDocOutDir = docsOutDir.map { it.dir("asciidoc") }
val asciiDocManPageOutDir = asciiDocOutDir.map { it.dir("manpage") }
val manpageOutDir = docsOutDir.map { it.dir("manpage/output") }
val htmlOutDir = docsOutDir.map { it.dir("html") }
val ghPagesOutDir = docsOutDir.map { it.dir("gh-pages") }
val ghPagesVersionedOutDir = ghPagesOutDir.map { it.dir("versioned") }
val ghPagesStaticOutDir = ghPagesOutDir.map { it.dir("static") }
val actionSchemaOutDir = ghPagesStaticOutDir.map { it.dir("schemas/action") }

// Prepare directories
val prepare = tasks.register("prepare") {
    group = "documentation"
    description = "Create documentation output directories"
    dependsOn("build")
    outputs.dirs(
        asciiDocOutDir,
        asciiDocManPageOutDir,
        manpageOutDir,
        htmlOutDir,
        ghPagesOutDir,
        actionSchemaOutDir
    )
    doLast {
        listOf(
            docsOutDir,
            asciiDocOutDir,
            asciiDocManPageOutDir,
            manpageOutDir,
            htmlOutDir,
            ghPagesOutDir,
            actionSchemaOutDir
        ).forEach { it.get().asFile.mkdirs() }
    }
}

// Action schema generation
val fcliActionSchemaVersion = project.property("fcliActionSchemaVersion")
val generateActionSchema = tasks.register<JavaExec>("generateActionSchema") {
    group = "documentation"
    description = "Generate fcli action JSON schema"
    dependsOn(prepare)
    inputs.property("projectVersion", project.version)
    inputs.property("schemaVersion", fcliActionSchemaVersion)
    outputs.dir(actionSchemaOutDir)
    classpath(sourceSets.main.get().runtimeClasspath, configurations.runtimeClasspath)
    mainClass.set("com.fortify.cli.common.action.schema.generator.GenerateActionSchema")
    args(project.version.toString().startsWith("0."), fcliActionSchemaVersion, actionSchemaOutDir.get().asFile.absolutePath)
}

// Replace original generateAsciiDocManPage implementation with two-step process avoiding deprecated project.javaexec
val fcliRootCommandsClassName = project.property("fcliRootCommandsClassName") as String
val manPageDocPropsFile = layout.buildDirectory.file("generated-docs/manpage/docprops.properties")

// First, collect doc properties output
val collectManPageDocProps = tasks.register<JavaExec>("collectManPageDocProps") {
    group = "documentation"
    description = "Collect man page doc properties"
    dependsOn(prepare)
    inputs.property("rootCommandsClassName", fcliRootCommandsClassName)
    outputs.file(manPageDocPropsFile)
    val capture = ByteArrayOutputStream()
    standardOutput = capture
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("com.fortify.cli.app.runner.util.FortifyCLIResourceBundlePropertiesHelper")
    doLast {
        val propsLines = capture.toString().lineSequence()
            .filter { it.contains(":") }
            .map { it.split(":", limit = 2) }
            .joinToString("\n") { (k,v) -> "$k=$v" }
        val file = manPageDocPropsFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(propsLines)
    }
}

// Second, generate the intermediate AsciiDoc man pages using collected properties
val generateAsciiDocManPage = tasks.register<JavaExec>("generateAsciiDocManPage") {
    group = "documentation"
    description = "Generate intermediate AsciiDoc man pages"
    dependsOn(collectManPageDocProps)
    inputs.file(manPageDocPropsFile)
    outputs.dir(asciiDocManPageOutDir)
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("picocli.codegen.docgen.manpage.ManPageGenerator")
    doFirst {
        val props = manPageDocPropsFile.get().asFile.readLines().mapNotNull { line ->
            if (!line.contains("=")) null else line.split("=", limit = 2).let { it[0] to it[1] }
        }.toMap()
        systemProperties(props)
        args = listOf(fcliRootCommandsClassName, "--outdir=${asciiDocManPageOutDir.get().asFile}", "-v")
    }
}

fun registerAsciiDocGenerator(name: String, description: String, argsProvider: () -> List<String>) =
    tasks.register<JavaExec>(name) {
        group = "documentation"
        this.description = description
        dependsOn(generateAsciiDocManPage)
        outputs.dir(asciiDocOutDir)
        classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
        mainClass.set("com.fortify.cli.app.FortifyCLI")
        args(argsProvider())
    }

val generateAsciiDocGenericActions = registerAsciiDocGenerator(
    "generateAsciiDocGenericActions",
    "Generate Generic Action AsciiDoc"
) { listOf("action", "asciidoc", "-d=${asciiDocManPageOutDir.get().asFile}", "-f=${asciiDocOutDir.get().asFile}/generic-actions.adoc") }

val generateAsciiDocSSCActions = registerAsciiDocGenerator(
    "generateAsciiDocSSCActions",
    "Generate SSC Action AsciiDoc"
) { listOf("ssc", "action", "asciidoc", "-d=${asciiDocManPageOutDir.get().asFile}", "-f=${asciiDocOutDir.get().asFile}/ssc-actions.adoc") }

val generateAsciiDocFoDActions = registerAsciiDocGenerator(
    "generateAsciiDocFoDActions",
    "Generate FoD Action AsciiDoc"
) { listOf("fod", "action", "asciidoc", "-d=${asciiDocManPageOutDir.get().asFile}", "-f=${asciiDocOutDir.get().asFile}/fod-actions.adoc") }

val generateAsciiDocActionDevelopment = tasks.register<JavaExec>("generateAsciiDocActionDevelopment") {
    group = "documentation"
    description = "Generate Action Development AsciiDoc"
    dependsOn(generateAsciiDocManPage)
    outputs.dir(asciiDocOutDir)
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("com.fortify.cli.app.FortifyCLI")
    args(
        "action", "run", "--on-unsigned=ignore", "--on-invalid-version=ignore",
        "${projectDir}/src/actions/generate-action-dev-doc.yaml",
        "--file=${asciiDocOutDir.get().asFile}/action-development.adoc"
    )
}

val generateAsciiDocAll = tasks.register<Copy>("generateAsciiDocAll") {
    group = "documentation"
    description = "Aggregate all generated AsciiDoc files"
    dependsOn(
        generateAsciiDocActionDevelopment,
        generateAsciiDocGenericActions,
        generateAsciiDocSSCActions,
        generateAsciiDocFoDActions
    )
    from(versionedAsciiDocSrcDir) { include("*.adoc") }
    into(asciiDocOutDir)
    inputs.dir(versionedAsciiDocSrcDir)
    outputs.dir(asciiDocOutDir)
}

fun registerAsciidoctorTask(
    name: String,
    depends: TaskProvider<*>,
    outputDirProvider: Provider<Directory>,
    backend: String,
    source: () -> java.io.File
) = tasks.register<AsciidoctorTask>(name) {
    group = "documentation"
    description = "Generate $name output"
    dependsOn(depends)
    setSourceDir(source())
    setOutputDir(outputDirProvider.get().asFile)
    outputs.dir(outputDirProvider)
    outputOptions { backends(backend) }
    val schemaUrl = rootProject.extra["fcliActionSchemaUrl"].toString()
    when (name) {
        "asciiDoctorVersionedHtml", "asciiDoctorVersionedJekyll" -> attributes(
            mapOf(
                "toc" to "left", "sectanchors" to "true", "docinfo" to "shared", "jekyll" to (name == "asciiDoctorVersionedJekyll"),
                "bannertitle" to "FCLI: The Universal Fortify CLI", "docversion" to project.version.toString(),
                "actionSchemaVersion" to fcliActionSchemaVersion.toString(), "actionSchemaUrl" to schemaUrl
            )
        )
        "asciiDoctorStaticJekyll" -> attributes(
            mapOf(
                "toc" to "left", "sectanchors" to "true", "docinfo" to "shared", "jekyll" to true, "stylesheet" to false,
                "bannertitle" to "FCLI: The Universal Fortify CLI", "docversion" to "[select]", "revnumber" to "none"
            )
        )
        else -> { /* no extra attributes */ }
    }
}

val generateManpageOutput = registerAsciidoctorTask(
    "generateManpageOutput", generateAsciiDocAll, manpageOutDir, "manpage"
) { asciiDocManPageOutDir.get().asFile }
val asciiDoctorVersionedHtml = registerAsciidoctorTask(
    "asciiDoctorVersionedHtml", generateAsciiDocAll, htmlOutDir, "html5"
) { asciiDocOutDir.get().asFile }
val asciiDoctorVersionedJekyll = registerAsciidoctorTask(
    "asciiDoctorVersionedJekyll", generateAsciiDocAll, ghPagesVersionedOutDir, "html5"
) { asciiDocOutDir.get().asFile }
val asciiDoctorStaticJekyll = registerAsciidoctorTask(
    "asciiDoctorStaticJekyll", generateActionSchema, ghPagesStaticOutDir, "html5"
) { staticAsciiDocSrcDir.asFile }

fun registerDocsZip(name: String, dep: TaskProvider<*>, archive: String, fromDir: Provider<Directory>, toDist: Boolean = false) =
    tasks.register<Zip>(name) {
        group = "distribution"
        description = "Package $name output"
        dependsOn(dep)
        archiveFileName.set(archive)
        val dest = if (toDist) file(rootProject.extra["distDir"] as String) else (rootProject.extra["releaseAssetsDir"] as? String)?.let { file(it) } ?: file(rootProject.extra["distDir"] as String)
        destinationDirectory.set(dest)
        from(fromDir)
        outputs.file(dest.resolve(archive))
    }

val distDocsVersionedHtml = registerDocsZip("distDocsVersionedHtml", asciiDoctorVersionedHtml, "docs-html.zip", htmlOutDir)
val distDocsManpage = registerDocsZip("distDocsManpage", generateManpageOutput, "docs-manpage.zip", manpageOutDir)
val distDocsVersionedJekyll = registerDocsZip("distDocsVersionedJekyll", asciiDoctorVersionedJekyll, "docs-gh-pages-versioned.zip", ghPagesVersionedOutDir, toDist = true)
val distDocsStaticJekyll = registerDocsZip("distDocsStaticJekyll", asciiDoctorStaticJekyll, "docs-gh-pages-static.zip", ghPagesStaticOutDir, toDist = true)

tasks.register("dist") {
    group = "distribution"
    description = "Build all documentation distribution archives"
    dependsOn(distDocsVersionedHtml, distDocsManpage, distDocsVersionedJekyll, distDocsStaticJekyll)
}