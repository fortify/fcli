plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("fcliJavaConventions") {
            id = "fcli.java-conventions"
            implementationClass = "com.fortify.fcli.buildlogic.FcliJavaConventionsPlugin"
        }
        create("fcliModuleConventions") {
            id = "fcli.module-conventions"
            implementationClass = "com.fortify.fcli.buildlogic.FcliModuleConventionsPlugin"
        }
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("io.freefair.gradle:lombok-plugin:8.13")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.52.0")
    // Allow referencing org.asciidoctor.gradle.jvm.AsciidoctorTask in conventions plugin
    implementation("org.asciidoctor:asciidoctor-gradle-jvm:4.0.4")
}