plugins { id("fcli.module-conventions") }

dependencies {
    implementation(project(property("fcliCommonActionRef") as String))
}