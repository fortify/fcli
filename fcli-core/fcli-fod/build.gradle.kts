plugins { id("fcli.module-conventions") }

dependencies {
    val aviatorCommonRef = project.findProperty("fcliAviatorCommonRef") as String
    implementation(project(aviatorCommonRef))
}
