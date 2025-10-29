plugins { id("fcli.module-conventions") }

// Build-time action to generate CI env vars output
val buildTimeActionCiEnvvars = tasks.register<JavaExec>("buildTimeAction_ci_envvars") {
    group = "build resources"
    description = "Generate build-time CI environment variables action output"
    systemProperty("fcli.terminal.width", "80") // Set text table width to 80 characters
    val outputDirProvider = layout.buildDirectory.dir("generated-action-output-resources")
    val ciEnvVarsLog = layout.buildDirectory.file("ci-envvars.log")
    val inputYaml = project.layout.projectDirectory.file("src/main/resources/com/fortify/cli/generic_action/actions/build-time/ci-envvars.yaml")
    inputs.file(inputYaml)
    inputs.property("projectVersion", project.version)
    outputs.dir(outputDirProvider)
    doFirst { outputDirProvider.get().asFile.mkdirs() }
    // Use dependency classpath excluding this project's own output to avoid circular dependency
    val runtimeCp = configurations.runtimeClasspath.get()
    classpath = runtimeCp.filter { !it.path.contains("/build/classes/") } + files(configurations.annotationProcessor.get())
    mainClass.set("com.fortify.cli.common.action.cli.cmd.RunBuildTimeFcliAction")
    doFirst {
        args = listOf(ciEnvVarsLog.get().asFile.absolutePath, inputYaml.asFile.absolutePath, "-d", outputDirProvider.get().asFile.absolutePath)
    }
}