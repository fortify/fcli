plugins { id("fcli.module-conventions") }

dependencies {
    val debrickedRef = project.findProperty("fcliDebrickedRef") as String
    implementation(project(debrickedRef))
}