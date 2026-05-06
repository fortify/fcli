plugins {
    id("fcli.java-conventions")
    id("groovy")
    id("com.github.johnrengelman.shadow")
}

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

repositories { mavenCentral() }

testing {
    suites {
        register<JvmTestSuite>("ftest") {
            useJUnitJupiter()
            dependencies {
                implementation(platform("org.apache.groovy:groovy-bom:4.0.20"))
                implementation("org.apache.groovy:groovy")
                implementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
                implementation("org.spockframework:spock-core")
                implementation("org.junit.platform:junit-platform-launcher:1.10.2")
                val fcliProp = project.findProperty("ftest.fcli")?.toString()
                if (fcliProp == null || fcliProp == "build") {
                    val appRef = project.findProperty("fcliAppRef") as String
                    implementation(project(appRef))
                }
            }
            targets { all { testTask.configure {
                // Pass all ft.* or ftest.* system properties through
                val passProps = System.getProperties().stringPropertyNames()
                    .filter { it.startsWith("ft.") || it.startsWith("ftest.") }
                    .associateWith { System.getProperty(it) }
                systemProperties(passProps)
                testLogging { showStandardStreams = false }
            } } }
        }
    }
}

// Shadow jar containing functional tests
val ftestShadowJar = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("ftestShadowJar") {
    group = "verification"
    description = "Package functional test suite into a runnable shadow jar"
    archiveBaseName.set("fcli")
    archiveClassifier.set("ftest")
    archiveVersion.set("")
    // The Jvm Test Suite creates a source set named ftest
    from(sourceSets.named("ftest").get().output)
    configurations = listOf(project.configurations.getByName("ftestRuntimeClasspath"))
    manifest { attributes["Main-Class"] = "com.fortify.cli.ftest.TestRunner" }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Distribution task for functional test jar
val distFtest = tasks.register<Copy>("distFtest") {
    dependsOn(ftestShadowJar, "createDistDir")
    val distDir = rootProject.extra["distDir"] as String
    into(distDir)
    from(ftestShadowJar.map { it.archiveFile }) {
        rename { _ -> "fcli-ftest.jar" }
    }
}

// Ensure root aggregate keeps working (root build already depends on :fcli-other:fcli-functional-test:distFtest)

// Optional task implemented as a custom type to allow ExecOperations injection and avoid deprecated Project.exec()
abstract class ReportFunctionalTestCommandCoverageTask : DefaultTask() {
    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        if ((project.findProperty("skipCommandCoverage") as String?)?.toBoolean() == true) {
            logger.lifecycle("Skipping command coverage report (skipCommandCoverage=true)")
            return
        }
        data class CommandData(val command:String, val aliases:Set<String>)
        data class Gap(val command:String, val aliases:Set<String>)
        fun parseCsvLine(lineRaw:String): CommandData? {
            var line = lineRaw.trim().trimEnd(',')
            if (line.isEmpty() || !line.startsWith('"')) return null
            val closing = line.indexOf('"', 1)
            if (closing<=1) return null
            val primary = line.substring(1, closing)
            var aliasesField = ""
            if (closing+1 < line.length && line[closing+1]==',') {
                val rem = line.substring(closing+2).trim()
                if (rem.startsWith('"') && rem.endsWith('"') && rem.length>=2) {
                    aliasesField = rem.substring(1, rem.length-1)
                }
            }
            val aliasSet = aliasesField.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            aliasSet += primary
            return CommandData(primary, aliasSet)
        }
        val appRef = project.findProperty("fcliAppRef") as String
        val appPath = if (appRef.startsWith(":")) appRef else ":$appRef"
        val appProj = project.project(appPath)
        val jarFile = appProj.layout.buildDirectory.file("libs/fcli.jar").get().asFile
        if (!jarFile.exists()) throw GradleException("Unable to locate fcli.jar at ${jarFile.path}")
        val out = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("java", "-jar", jarFile.absolutePath, "util", "all-commands", "ls", "-q", "runnable && !hidden", "-o", "csv=command,fullAliasesString")
            standardOutput = out
        }
        val csv = out.toString(StandardCharsets.UTF_8)
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) throw GradleException("No output from all-commands list; cannot compute coverage")
        val commands = lines.drop(1).mapNotNull { parseCsvLine(it) }
        if (commands.isEmpty()) throw GradleException("Parsed zero commands; CSV format may have changed")
        val srcRoot = project.file("src")
        val sources = if (srcRoot.exists()) srcRoot.walkTopDown().filter { f -> f.isFile && !f.name.startsWith('.') }.toList() else emptyList()
        val contents = sources.map { it.readText() }
        val covered = mutableSetOf<String>()
        val gaps = mutableListOf<Gap>()
        commands.forEach { c ->
            val variants = c.aliases.flatMap { a -> if (a.startsWith("fcli ")) listOf(a, a.removePrefix("fcli ")) else listOf(a) }
            val isCovered = variants.any { v -> contents.any { text -> text.contains(v) } }
            if (isCovered) covered += c.command else gaps += Gap(c.command, c.aliases)
        }
        val total = commands.size
        val coveredCount = covered.size
        val uncoveredCount = gaps.size
        val coveragePct = if (total==0) 0.0 else coveredCount.toDouble()/total.toDouble()*100.0
        val reportDir = project.layout.buildDirectory.dir("reports/commandCoverage").get().asFile.apply { mkdirs() }
        val reportFile = reportDir.resolve("functional-test-command-coverage.html")
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang=\"en\">")
        sb.appendLine("<head><meta charset=\"UTF-8\"/><title>fcli Functional Test Command Coverage</title><style>body{font-family:Arial,Helvetica,sans-serif;margin:1.5rem;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ccc;padding:4px 8px;font-size:13px;} th{background:#f6f6f6;text-align:left;} .metrics{margin-bottom:1rem;} .pct{font-weight:bold;} .uncovered-count{color:#c00;} .covered-count{color:#060;} code{background:#f5f5f5;padding:2px 4px;border-radius:3px;} </style></head>")
        sb.appendLine("<body>")
        sb.appendLine("<h1>Functional Test Coverage Gaps</h1>")
        sb.appendLine("<div class=\"metrics\">")
        sb.appendLine("<p>Total runnable non-hidden commands: <strong>$total</strong></p>")
        sb.appendLine("<p class=\"covered-count\">Covered: <strong>$coveredCount</strong></p>")
        sb.appendLine("<p class=\"uncovered-count\">Not covered: <strong>$uncoveredCount</strong></p>")
        sb.appendLine("<p class=\"pct\">Coverage: <strong>${"%.1f".format(coveragePct)}%</strong></p>")
        sb.appendLine("</div>")
        sb.appendLine("<h2>Uncovered Commands</h2>")
        sb.appendLine("<table><thead><tr><th>Command</th></tr></thead><tbody>")
        gaps.sortedBy { it.command }.forEach { g -> sb.appendLine("<tr><td><code>${g.command}</code></td></tr>") }
        sb.appendLine("</tbody></table>")
        sb.appendLine("<p>Generated: ${Instant.now()}</p>")
        sb.appendLine("</body></html>")
        reportFile.writeText(sb.toString())
        logger.lifecycle("Functional test command coverage HTML report written to: ${reportFile.relativeTo(project.rootProject.projectDir)}")
        logger.lifecycle("Coverage summary: $coveredCount/$total (${"%.1f".format(coveragePct)}%) covered; $uncoveredCount uncovered")
    }
}

tasks.register<ReportFunctionalTestCommandCoverageTask>("reportFunctionalTestCommandCoverage") {
    group = "verification"
    description = "Generate HTML report listing uncovered runnable non-hidden commands (no aliases)"
    // Ensure app jar is available first
    val appRef = project.findProperty("fcliAppRef") as String
    val appPath = if (appRef.startsWith(":")) appRef else ":$appRef"
    dependsOn("$appPath:build")
}