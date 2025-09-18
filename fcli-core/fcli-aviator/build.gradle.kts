plugins { id("fcli.module-conventions") }

dependencies {
    val aviatorCommonRef = project.findProperty("fcliAviatorCommonRef") as String
    val sscRef = project.findProperty("fcliSSCRef") as String
    val fodRef = project.findProperty("fcliFoDRef") as String
    implementation(project(aviatorCommonRef))
    implementation(project(sscRef))
    implementation(project(fodRef))
}
