plugins {
    id("java-platform")
    id("com.github.ben-manes.versions")
}

description = "Bill Of Materials for fcli"

javaPlatform { allowDependencies() }

dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.22.4"))
    api(platform("org.springframework:spring-framework-bom:7.0.9"))
    api(platform("io.modelcontextprotocol.sdk:mcp-bom:1.1.1"))
    constraints {
        api("info.picocli:picocli:4.7.5")
        api("info.picocli:picocli-codegen:4.7.7")
        api("com.formkiq:graalvm-annotations:1.2.0")
        api("com.formkiq:graalvm-annotations-processor:1.5.2")
        api("org.fusesource.jansi:jansi:2.4.3")
        api("org.slf4j:slf4j-api:2.0.20")
        api("org.slf4j:jcl-over-slf4j:2.0.20")
        api("ch.qos.logback:logback-classic:1.6.5")
        api("com.konghq:unirest-java:3.14.5")
        api("com.konghq:unirest-objectmapper-jackson:3.14.5")
        api("com.github.freva:ascii-table:1.13.0")
        api("com.google.code.findbugs:jsr305:3.0.2")
        api("org.jasypt:jasypt:1.9.3:lite")
        api("org.junit.jupiter:junit-jupiter-api:6.1.3")
        api("org.junit.jupiter:junit-jupiter-params:6.1.3")
        api("org.junit.jupiter:junit-jupiter-engine:6.1.3")
        api("org.apache.commons:commons-lang3:3.21.0")
        api("org.apache.commons:commons-compress:1.28.0")
        api("org.jsoup:jsoup:1.23.2")
        api("org.eclipse.jgit:org.eclipse.jgit:7.8.0.202609011348-r")
    }
}
