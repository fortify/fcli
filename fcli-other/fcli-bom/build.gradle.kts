plugins {
    id("java-platform")
    id("com.github.ben-manes.versions")
}

description = "Bill Of Materials for fcli"

javaPlatform { allowDependencies() }

dependencies {
    api(platform("com.fasterxml.jackson:jackson-bom:2.20.0"))
    api(platform("org.springframework:spring-framework-bom:6.2.11"))
    api(platform("io.modelcontextprotocol.sdk:mcp-bom:0.14.0"))
    constraints {
        api("info.picocli:picocli:4.7.5")
        api("info.picocli:picocli-codegen:4.7.5")
        api("com.formkiq:graalvm-annotations:1.2.0")
        api("com.formkiq:graalvm-annotations-processor:1.5.1")
        api("org.fusesource.jansi:jansi:2.4.2")
        api("org.slf4j:slf4j-api:2.0.12")
        api("org.slf4j:jcl-over-slf4j:2.0.12")
        api("ch.qos.logback:logback-classic:1.5.3")
        api("com.konghq:unirest-java:3.14.5")
        api("com.konghq:unirest-objectmapper-jackson:3.14.5")
        api("com.github.freva:ascii-table:1.8.0")
        api("com.google.code.findbugs:jsr305:3.0.2")
        api("org.jasypt:jasypt:1.9.3:lite")
        api("org.junit.jupiter:junit-jupiter-api:5.13.4")
        api("org.junit.jupiter:junit-jupiter-params:5.13.4")
        api("org.junit.jupiter:junit-jupiter-engine:5.13.4")
        api("org.apache.commons:commons-lang3:3.19.0")
        api("org.apache.commons:commons-compress:1.28.0")
        api("org.jsoup:jsoup:1.21.2")
        api("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
    }
}