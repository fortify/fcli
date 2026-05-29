plugins { id("fcli.module-conventions") }

dependencies {
    implementation(project(property("fcliCommonCiRef") as String))
}
