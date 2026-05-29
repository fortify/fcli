package com.fortify.fcli.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class FcliModuleConventionsPlugin: Plugin<Project> {
    override fun apply(project: Project) = project.run {
        plugins.apply("fcli.java-conventions")
        fun addIfNotSelf(propName: String) {
            val ref = findProperty(propName) as String?
            if (ref != null && ref != path) {
                dependencies.add("implementation", project(ref))
            }
        }
        addIfNotSelf("fcliCommonRef")
        addIfNotSelf("fcliCommonThirdpartyRef")
    }
}
