plugins {
    id("fcli.module-conventions")
    id("java-library")
    id("java-test-fixtures")
    id("com.google.protobuf")
}

java {
    withSourcesJar()
}

// Ensure proto tasks run before Java compilation
tasks.withType<JavaCompile>().configureEach { dependsOn("generateProto") }

dependencies {
    val fprRef = project.findProperty("fcliCommonFprRef") as String
    api(project(fprRef))
    implementation(project(":fcli-core:fcli-common-core"))
    testFixturesImplementation(project(":fcli-core:fcli-common-core"))
    implementation("org.yaml:snakeyaml:2.3")

    // Note: StAX (javax.xml.stream) uses Woodstox 7.1.1 via jackson-dataformat-xml
    // from fcli-common (needed for XML output). No explicit dependency required.

    implementation("com.auth0:java-jwt:4.5.0")
    implementation("io.grpc:grpc-netty-shaded:1.76.0")
    implementation("io.grpc:grpc-protobuf:1.76.0")
    api("io.grpc:grpc-stub:1.76.0")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
    api("com.google.protobuf:protobuf-java:4.28.3")
    api("com.google.protobuf:protobuf-java-util:4.28.3")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:4.28.3" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.76.0" }
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
