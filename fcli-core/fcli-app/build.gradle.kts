plugins {
    id("fcli.java-conventions")
    id("application")
    id("com.github.johnrengelman.shadow")
    id("com.github.jk1.dependency-license-report")
}

// Inter-project dependencies
val refs = listOf(
    "fcliCommonRef","fcliActionRef","fcliAviatorRef","fcliConfigRef","fcliFoDRef","fcliSSCRef","fcliSCSastRef","fcliSCDastRef","fcliToolRef","fcliLicenseRef","fcliUtilRef"
)
references@ for (r in refs) {
    val p = project.findProperty(r) as String? ?: continue@references
    dependencies.add("implementation", project(p))
}

// Runtime-only additions
dependencies {
    runtimeOnly("org.slf4j:jcl-over-slf4j")
    runtimeOnly("org.fusesource.jansi:jansi")
}

// Picocli reflect config generation
val generatedReflectConfigDir = layout.buildDirectory.dir("generated-reflect-config")

val generatePicocliReflectConfig = tasks.register<JavaExec>("generatePicocliReflectConfig") {
    group = "code generation"
    description = "Generate picocli reflect-config.json"
    val outputFile = layout.buildDirectory.file("generated-reflect-config/META-INF/native-image/picocli-reflect-config/reflect-config.json")
    inputs.property("rootCommandsClassName", project.property("fcliRootCommandsClassName"))
    inputs.files(configurations.runtimeClasspath, configurations.annotationProcessor, sourceSets.main.get().runtimeClasspath)
    outputs.file(outputFile)
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor, sourceSets.main.get().runtimeClasspath)
    mainClass.set("picocli.codegen.aot.graalvm.ReflectionConfigGenerator")
    args(project.property("fcliRootCommandsClassName"), "-o", outputFile.get().asFile.absolutePath)
}

// Generate reflect-config.json for MCP-related classes
val generateMCPReflectConfig = tasks.register<JavaExec>("generateMCPReflectConfig") {
    group = "code generation"
    description = "Generate MCP reflect-config.json"
    val outputFile = layout.buildDirectory.file("generated-reflect-config/META-INF/native-image/mcp-reflect-config/reflect-config.json")
    inputs.files(configurations.runtimeClasspath, sourceSets.main.get().runtimeClasspath)
    outputs.file(outputFile)
    classpath(configurations.runtimeClasspath, sourceSets.main.get().runtimeClasspath)
    mainClass.set("com.fortify.cli.util.mcp_server.helper.mcp.MCPReflectConfigGenerator")
    args(outputFile.get().asFile.absolutePath)
}

application { mainClass.set(project.property("fcliMainClassName") as String) }

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    dependsOn(generatePicocliReflectConfig, generateMCPReflectConfig)
    mergeServiceFiles()
    archiveBaseName.set("fcli")
    archiveClassifier.set("")
    archiveVersion.set("")
    from(generatedReflectConfigDir)
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Third-party helper
val gradleHelpersLocation = rootProject.extra["gradleHelpersLocation"] as String
apply(from = "${gradleHelpersLocation}/thirdparty-helper.gradle")

tasks.register("distThirdPartyReleaseAsset") {
    group = "distribution"
    description = "Prepare third-party license asset in release assets directory"
    dependsOn("distThirdParty", "createDistDir")
    val producedName = providers.provider { "fcli-${project.version}-thirdparty.zip" }
    val src = layout.buildDirectory.file(providers.provider { "dist/${producedName.get()}" })
    inputs.file(src)
    val releaseAssetsDir = rootProject.extra["releaseAssetsDir"] as String
    val target = file("${releaseAssetsDir}/fcli-thirdparty.zip")
    outputs.file(target)
    doLast {
        val srcFile = src.get().asFile
        if (!srcFile.exists()) throw GradleException("Expected third-party archive not found: $srcFile")
        if (target.exists()) target.delete()
        srcFile.copyTo(target, overwrite = true)
    }
}

tasks.register<Copy>("dist") {
    group = "distribution"
    description = "Copy application shadow jar to release assets directory"
    dependsOn("shadowJar", "createDistDir")
    from(layout.buildDirectory.dir("libs")) { include("fcli.jar") }
    into(rootProject.layout.buildDirectory.dir("dist/release-assets"))
    inputs.file(layout.buildDirectory.file("libs/fcli.jar"))
    outputs.dir(rootProject.layout.buildDirectory.dir("dist/release-assets"))
}