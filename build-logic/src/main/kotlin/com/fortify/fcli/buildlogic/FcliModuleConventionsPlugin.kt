package com.fortify.fcli.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class FcliModuleConventionsPlugin: Plugin<Project> {
    override fun apply(project: Project) = project.run {
        plugins.apply("fcli.java-conventions")
        afterEvaluate {
            val commonRef = findProperty("fcliCommonRef") as String?
            if (commonRef != null && commonRef != path) {
                dependencies.add("implementation", project(commonRef))
            }
        }
    }
}
