plugins { id("fcli.module-conventions") }

dependencies {
	val fodRef = project.findProperty("fcliFoDRef") as String
	val sscRef = project.findProperty("fcliSSCRef") as String
	val toolRef = project.findProperty("fcliToolRef") as String
	implementation(project(fodRef))
	implementation(project(sscRef))
	implementation(project(toolRef))
}
