plugins { id("fcli.module-conventions") }

@Suppress("UNCHECKED_CAST")
val registerActionZipTask = project.extra["registerActionZipTask"] as (Map<String, Any?>) -> Unit

registerActionZipTask(mapOf(
    "name" to "zipResources_actions",
    "src" to "src/main/resources/com/fortify/cli/fod/actions/zip",
    "dest" to "com/fortify/cli/fod"
))
