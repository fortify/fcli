plugins { id("fcli.module-conventions") }

dependencies {
    val aviatorCommonRef = project.findProperty("fcliAviatorCommonRef") as String
    val sscRef = project.findProperty("fcliSSCRef") as String
    implementation(project(aviatorCommonRef))
    implementation(project(sscRef))
}
