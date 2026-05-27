plugins { id("fcli.module-conventions") }

dependencies {
    val commonCiRef = project.findProperty("fcliCommonCiRef") as String
    val sscRef = project.findProperty("fcliSSCRef") as String
    implementation(project(commonCiRef))
    implementation(project(sscRef))
}
