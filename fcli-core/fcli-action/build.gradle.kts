plugins { id("fcli.module-conventions") }

// Build-time action to generate CI documentation output
val buildTimeActionCiDoc = tasks.register<JavaExec>("buildTimeAction_ci_doc") {
    group = "build resources"
    description = "Generate build-time CI documentation action output"
    systemProperty("fcli.terminal.width", "80") // Set text table width to 80 characters
    val outputDirProvider = layout.buildDirectory.dir("generated-action-output-resources")
    val ciDocLog = layout.buildDirectory.file("ci-doc.log")
    val inputYaml = project.layout.projectDirectory.file("src/main/resources/com/fortify/cli/generic_action/actions/build-time/ci-doc.yaml")
    inputs.file(inputYaml)
    inputs.property("projectVersion", project.version)
    outputs.dir(outputDirProvider)
    doFirst { outputDirProvider.get().asFile.mkdirs() }
    // Use dependency classpath excluding this project's own output to avoid circular dependency
    val runtimeCp = configurations.runtimeClasspath.get()
    classpath = runtimeCp.filter { !it.path.contains("/build/classes/") } + files(configurations.annotationProcessor.get())
    mainClass.set("com.fortify.cli.common.action.cli.cmd.RunBuildTimeFcliAction")
    doFirst {
        args = listOf(ciDocLog.get().asFile.absolutePath, inputYaml.asFile.absolutePath, "-d", outputDirProvider.get().asFile.absolutePath)
    }
}