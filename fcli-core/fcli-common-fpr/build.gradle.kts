plugins {
    id("fcli.module-conventions")
    id("java-library")
}

// Standalone FPR parsing library: must not depend on any Aviator module.
dependencies {
    implementation("org.yaml:snakeyaml:2.3")
}
