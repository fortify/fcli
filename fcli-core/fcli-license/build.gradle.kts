plugins { id("fcli.module-conventions") }

dependencies {
    val sscRef = project.findProperty("fcliSSCRef") as String
    implementation(project(sscRef))
}
