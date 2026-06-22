plugins { id("fcli.module-conventions") }

dependencies {
    val commonActionRef = project.findProperty("fcliCommonActionRef") as String
    val aviatorCommonRef = project.findProperty("fcliAviatorCommonRef") as String
    implementation(project(commonActionRef))
    implementation(project(aviatorCommonRef))
}
