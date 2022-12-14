<x-tag-head>
<x-tag-meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.0.0/build/highlight.min.js"/>
--></x-tag-script>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js" />
--></x-tag-script>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="${gradleHelpersLocation}/spa_readme.js" />
--></x-tag-script>

<x-tag-style><!--
<X-INCLUDE url="https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.0.0/build/styles/github.min.css" />
--></x-tag-style>

<x-tag-style><!--
<X-INCLUDE url="${gradleHelpersLocation}/spa_readme.css" />
--></x-tag-style>
</x-tag-head>

# Fortify CLI

## Introduction

Build secure software fast with [Fortify](https://www.microfocus.com/en-us/solutions/application-security). Fortify offers end-to-end application security solutions with the flexibility of testing on-premises and on-demand to scale and cover the entire software development lifecycle. With Fortify, find security issues early and fix at the speed of DevOps.

The Fortify Command Line Interface (fcli) is a command line tool that aims to be a single binary executable that allows you to interact with multiple Fortify products.
Currently, the fcli utility can be used to interact with various Fortify products, like Fortify on Demand (FoD), Software Security Center (SSC), ScanCentral SAST and ScanCentral DAST.

  
### Related Links

* **Downloads**: https://github.com/fortify-ps/fcli/releases
    * _Development releases may be unstable or non-functional. The `*-thirdparty.zip` file is for informational purposes only and does not need to be downloaded._
* **Docker images**: TBD
* **Source code**: https://github.com/fortify-ps/fcli
* **Automated builds**: https://github.com/fortify-ps/fcli/actions
* **FCLI Command Line Usage Documentation**: https://fortify-ps.github.io/fcli/


## Developers

The following sections provide information that may be useful for developers of this utility.

### Technologies & frameworks

Following is a list of the main frameworks and technologies in use:
* [picocli](https://picocli.info/): Process command line options, generate usage information, ...
* [Micronaut](https://micronaut.io/): Dependency injection, features for GraalVM native image generation
* [Jackson](https://github.com/FasterXML/jackson): Parse and generate data in JSON and other formats
* [GraalVM](https://www.graalvm.org/): Generate native images (native executables)

### Prerequisites & considerations

As can be seen in the [Technologies & frameworks](#technologies-frameworks) section, this is no ordinary Java project. Some of these technologies and frameworks require special prerequisites, precautions and other considerations to be taken into account to prevent compilation issues and runtime errors, as described below.

#### IDE Setup

This project uses the following frameworks that may require some special setup in order to have your IDE compile this project without errors:

* Lombok: Please see https://projectlombok.org/setup/overview for more information on how to add Lombok support to your IDE
* Micronaut & picocli: These frameworks require annotation processors to be run during builds; please see your IDE documentation on how to enable annotation processing

#### Incremental Compilation

Incremental compilation (for example in IDE or when doing a `gradle build` without `clean`) may leave behind old AOT artifacts causing exceptions when trying to run `fcli`. This is especially the case when renaming or moving classes, which may result in exceptions like the below:

```
Message: Error loading bean [com.fortify.cli.ssc.rest.unirest.SSCUnirestRunner]: com/fortify/cli/rest/unirest/AbstractUnirestRunner
...
Caused by: java.lang.NoClassDefFoundError: com/fortify/cli/rest/unirest/AbstractUnirestRunner
...
```

In this example, the AbstractUnirestRunner class was moved to a different package, but the now obsolete AOT-generated classes were still present. So, if at any time you see unexpected exceptions, please try to do a full clean/rebuild and run `fcli` again.

#### Reflection

GraalVM Native Image needs to know ahead-of-time the reflectively accessed program elements; see [GraalVM Native Image & Reflection](https://www.graalvm.org/reference-manual/native-image/Reflection/) for details. Micronaut allows for generating the necessary GraalVM reflection configuration using the `@ReflectiveAccess` annotation if necessary.

If a command runs fine on a regular JVM but not when running as a native image, then quite likely this is caused by missing reflection configuration which may be fixed by adding the `@ReflectiveAccess` annotation to the appropriate classes. Also see below for some reflection-related considerations specific to picocli and Jackson.

#### Picocli

Picocli and it's annotation processor should in theory generate the necessary reflection configuration as described above. However, the annotation processor doesn't seem to take class hierarchies into account; see [GraalVM: Options from superclass not visible](https://github.com/remkop/picocli/issues/1444). As a work-around, we can complement the picocli-generated reflection configuration by using Micronaut's `@ReflectiveAccess` annotation. In general the following classes should probably be annotated with `@ReflectiveAccess`:
* All classes annotated with `@Command` (this likely duplicates the reflection configuration generated by picocli, but doesn't hurt)
* All super-classes of all classes annotated with `@Command`, in particular if they contain any picocli annotations like `@Mixin`, `@ArgGroup`, or `@Option`
* All classes used as an `@Mixin` or `@ArgGroup`, and their super-classes

The following native image runtime behavior may indicate a missing `@ReflectiveAccess` annotation:
* Options and sub-commands not listed in help output, and not recognized when entered on the command line
* Exceptions related to picocli trying to access classes, methods and fields using reflection

#### Jackson

Jackson is heavily based on reflection to perform object serialization and deserialization. All classes that need to be (de-)serialized with Jackson therefore need to be annotated with `@ReflectiveAccess` to allow reflection-based (de-)serialization.

Note that Micronaut provides the `@Introspected` annotation that should allow for reflection-free (de-)serialization. However experience learns that there are just too many differences between reflection-based and introspection-based (de-)serialization. When running in a normal JVM these differences are often not visible as Jackson can easily fall back to reflection-based (de-)serialization. In native images, Jackson cannot fall back to reflection-based (de-)serialization unless properly configured, and even when properly configured there are still some differences in behavior. 

As such, it has been decided to disable Jackson introspection-based (de-)serialization using the `jackson.bean-introspection-module` configuration property in `application.yml`. This also means that we will not be using the `@Introspected` annotation on classes that need to be (de-)serialized with Jackson (unless of course introspection-based access is used for non Jackson-related purposes).

### Gradle Wrapper

It is strongly recommended to build this project using the included Gradle Wrapper scripts; using other Gradle versions may result in build errors and other issues.

The Gradle build uses various helper scripts from https://github.com/fortify-ps/gradle-helpers; please refer to the documentation and comments in included scripts for more information. 

### Common Commands

All commands listed below use Linux/bash notation; adjust accordingly if you are running on a different platform. All commands are to be executed from the main project directory.

* `./gradlew tasks --all`: List all available tasks
* Build: (plugin binary will be stored in `build/libs`)
	* `./gradlew clean build`: Clean and build the project
	* `./gradlew build`: Build the project without cleaning
	* `./gradlew dist distThirdParty`: Build distribution zip and third-party information bundle

### Automated Builds

This project uses GitHub Actions workflows to perform automated builds for both development and production releases. All pushes to the main branch qualify for building a production release. Commits on the main branch should use [Conventional Commit Messages](https://www.conventionalcommits.org/en/v1.0.0/); it is recommended to also use conventional commit messages on any other branches.

User-facing commits (features or fixes) on the main branch will trigger the [release-please-action](https://github.com/google-github-actions/release-please-action) to automatically create a pull request for publishing a release version. This pull request contains an automatically generated CHANGELOG.md together with a version.txt based on the conventional commit messages on the main branch. Merging such a pull request will automatically publish the production binaries and Docker images to the locations described in the [Related Links](#related-links) section.

Every push to a branch in the GitHub repository will also automatically trigger a development release to be built. By default, development releases are only published as build job artifacts. However, if a tag named `dev_<branch-name>` exists, then development releases are also published to the locations described in the [Related Links](#related-links) section. The `dev_<branch-name>` tag will be automatically updated to the commit that triggered the build.

## License
<x-insert text="<!--"/>

See [LICENSE.TXT](LICENSE.TXT)

<x-insert text="-->"/>

<x-include url="file:LICENSE.TXT"/>

