plugins { id("fcli.module-conventions") }

dependencies {
	val fodRef = project.findProperty("fcliFoDRef") as String
	val sscRef = project.findProperty("fcliSSCRef") as String
	implementation(project(fodRef))
	implementation(project(sscRef))
}
