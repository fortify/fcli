plugins { id("fcli.module-conventions") }

val refs = listOf("fcliFoDRef", "fcliSSCRef")
references@ for (r in refs) {
	val p = project.findProperty(r) as String? ?: continue@references
	dependencies.add("implementation", project(p))
}
