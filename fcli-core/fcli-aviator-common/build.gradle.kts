plugins {
    id("fcli.module-conventions")
    id("java-library")
    id("com.google.protobuf")
}

java {
    withSourcesJar()
}

// Ensure proto tasks run before Java compilation
tasks.withType<JavaCompile>().configureEach { dependsOn("generateProto") }

dependencies {
    implementation(project(":fcli-core:fcli-common"))
    implementation("org.yaml:snakeyaml:2.3")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:2.3.3")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.3")
    implementation("com.sun.activation:jakarta.activation:2.0.1")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:3.0.1")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("io.grpc:grpc-netty-shaded:1.69.0")
    implementation("io.grpc:grpc-protobuf:1.69.0")
    api("io.grpc:grpc-stub:1.69.0")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:3.0.2")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
    api("com.google.protobuf:protobuf-java:4.28.3")
    api("com.google.protobuf:protobuf-java-util:4.28.3")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.28.3" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.69.0" }
    }
    generateProtoTasks {
        all().configureEach {
            plugins { create("grpc") }
        }
    }
}

// Replace deprecated direct buildDir string usage with provider-based dirs
val generatedProtoBase = layout.buildDirectory.dir("generated/source/proto")
sourceSets.named("main") {
    java.srcDir(generatedProtoBase.map { it.dir("main/grpc") })
    java.srcDir(generatedProtoBase.map { it.dir("main/java") })
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}