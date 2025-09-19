plugins { id("fcli.java-conventions") }

dependencies {
    val appRef = project.findProperty("fcliAppRef") as String
    runtimeOnly(project(appRef))
}

val autoCompleteOutput = layout.buildDirectory.file("dist/fcli_completion")

val distAutoComplete = tasks.register<JavaExec>("distAutoComplete") {
    group = "distribution"
    description = "Generate fcli autocomplete script"
    dependsOn("createDistDir")
    inputs.property("rootCommandsClassName", project.property("fcliRootCommandsClassName"))
    inputs.files(configurations.runtimeClasspath, configurations.annotationProcessor)
    outputs.file(autoCompleteOutput)
    classpath(configurations.runtimeClasspath, configurations.annotationProcessor)
    mainClass.set("picocli.AutoComplete")
    args(project.property("fcliRootCommandsClassName"), "-f", "--completionScript=${autoCompleteOutput.get().asFile.absolutePath}")
}

tasks.register("dist") {
    group = "distribution"
    description = "Generate autocomplete distribution artifacts"
    dependsOn(distAutoComplete)
    outputs.file(autoCompleteOutput)
}
