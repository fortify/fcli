plugins {
    id("fcli.java-conventions")
    id("groovy")
    id("com.github.johnrengelman.shadow")
}

repositories { mavenCentral() }

testing {
    suites {
        register<JvmTestSuite>("ftest") {
            useJUnitJupiter()
            dependencies {
                implementation(platform("org.apache.groovy:groovy-bom:4.0.20"))
                implementation("org.apache.groovy:groovy")
                implementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
                implementation("org.spockframework:spock-core")
                implementation("org.junit.platform:junit-platform-launcher:1.10.2")
                val fcliProp = project.findProperty("ftest.fcli")?.toString()
                if (fcliProp == null || fcliProp == "build") {
                    val appRef = project.findProperty("fcliAppRef") as String
                    implementation(project(appRef))
                }
            }
            targets { all { testTask.configure {
                // Pass all ft.* or ftest.* system properties through
                val passProps = System.getProperties().stringPropertyNames()
                    .filter { it.startsWith("ft.") || it.startsWith("ftest.") }
                    .associateWith { System.getProperty(it) }
                systemProperties(passProps)
                testLogging { showStandardStreams = false }
            } } }
        }
    }
}

// Shadow jar containing functional tests
val ftestShadowJar = tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("ftestShadowJar") {
    group = "verification"
    description = "Package functional test suite into a runnable shadow jar"
    archiveBaseName.set("fcli")
    archiveClassifier.set("ftest")
    archiveVersion.set("")
    // The Jvm Test Suite creates a source set named ftest
    from(sourceSets.named("ftest").get().output)
    configurations = listOf(project.configurations.getByName("ftestRuntimeClasspath"))
    manifest { attributes["Main-Class"] = "com.fortify.cli.ftest.TestRunner" }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Distribution task for functional test jar
val distFtest = tasks.register<Copy>("distFtest") {
    dependsOn(ftestShadowJar, "createDistDir")
    val distDir = rootProject.extra["distDir"] as String
    into(distDir)
    from(ftestShadowJar.map { it.archiveFile }) {
        rename { _ -> "fcli-ftest.jar" }
    }
}

// Ensure root aggregate keeps working (root build already depends on :fcli-other:fcli-functional-test:distFtest)