---
layout: page
title: Fortify CLI (fcli) Installation & Usage
nav:
  - title: FCLI Installation & Usage
    permalink: 
  - title: Installation
    permalink: installation
  - title: Command Structure
    permalink: command-structure
  - title: Common Options
    permalink: common-options
  - title: Session Management
    permalink: session-management
  - title: Environment Variables
    permalink: environment-variables
  - title: Fcli Variables
    permalink: fcli-variables
  - title: Manual Pages
    permalink: manual-pages
  - title: Troubleshooting
    permalink: troubleshooting
---

# Fortify CLI (fcli) Installation & Usage
The fcli utility can be used to interact with various Fortify products, like FoD, SSC, ScanCentral SAST and ScanCentral DAST. This document describes installation and general usage of fcli. For a full listing of fcli commands and corresponding command line options, please see the man-pages corresponding to the fcli version that you are using, as listed in the [Manual Pages](#manual-pages) section.

## Installation

Download bundles for fcli are available on the [Releases](https://github.com/fortify-ps/fcli/releases) page, containing both development releases (named `Development Release - <branch> branch`) and final releases. In general, the use of a final release is recommended, unless you want to use any functionality that hasn't made it into a final release yet.

Each release comes with a list of assets:
- `docs-html.zip` & `docs-manpage.zip`: Manual pages in either HTML or manpage format
- `fcli-linux.tgz`, `fcli-mac.tgz` & `fcli-windows.zip`: Native binaries for each of the mentioned platforms
    - Note that some browsers by default will disallow downloading of `fcli-windows.zip`; please bypass the warning
    - Linux and Mac downloads include an `auto-completion` script that makes interactive fcli usage easier
- `fcli.jar`: Java version of fcli, which should be runnable on any platform that has Java 11+ installed
    - Note that in general, the native binaries are easier to invoke, offer better performance, and have the benefit of auto-completion capabilities on Linux & Mac
    - If you experience any unexpected behavior with native binaries, like commands or command line options not being listed or recognized, or technical error messages about methods or constructors not being found, please try with the Java version as well as it may be an issue specific to the native binaries
- `fcli-thirdparty.zip`: Third-party licenses and sources for license purposes; usually no need to download
- `LICENSE.TXT` & `README.md`: Some generic information and license for fcli

To install one of the binary distributions of fcli:
- Download the appropriate binary archive for your platform
- Extract the archive contents to a directory of your choosing
- For ease of use, add this directory to your operating system or shell PATH environment variable, or move the `fcli`/`fcli.exe` binary to a directory that is already on the PATH
- Run `source <extraction-dir>/fcli_completion` to install fcli auto command completion, allowing for use of the `<tab>` to get suggestions for fcli command and option names. You may want to add this to your shell startup script, such that fcli auto-completion is readily available in every shell

To install the `.jar` version of fcli, simply download `fcli.jar` and put in in a directory of your choosing, after which it can be executed using `java -jar path/to/fcli.jar`. You may want to set up a simple wrapper script/batch file or shell alias to make it slightly easier to invoke `fcli.jar`.

## Command Structure
Fcli provides a product-oriented command structure, with each product represented by a separate tree of subcommands. For example, the `fcli fod` command tree can be used to interact with Fortify on Demand (FoD), and the `fcli ssc` command tree can be used to interact with Fortify Software Security Center (SSC). There are also some non product-related command trees, like the `fcli config` command tree to manage fcli configuration.

To see what top-level fcli commands are available, you can use the `fcli --help` command. You can drill down into the command tree to see what sub-commands are available within a particular parent command, for example by running `fcli ssc --help` to see all `fcli ssc` sub-commands, or `fcli ssc session --help` to see all SSC session management commands.

If you don't have fcli up and running yet, you can also refer to the downloadable or online manual pages; refer to the [Manual Pages](#manual-pages) section for more information.

## Common Options
The following sections describe common options that are available on (most) fcli commands.

### -h | --help

This option can be used on every fcli (sub-)command to view usage information for that command. Usage information usually shows the command synopsis, a description of the functionality provided by the command, and a description of each command line option or parameter accepted by the command.

### -V | --version

This option can be used on every fcli (sub-)command to view current fcli version. Currently, all sub-commands return the same version information.

### --log-level

This option can be used on every fcli (sub-)command to specify the fcli log level; see the help output for a list of allowed levels. Note that this option also requires the `--log-file` option to be specified, otherwise no log will be written.

### --log-file

This option can be used on every fcli (sub-)command to specify the file to which to output log data. If not specified, currently no log data will be written, although future versions may specify a default log file location in the fcli home folder.

### -o | --output

Available on virtually all (leaf) commands that output data, this option can be used to specify the output format. Fcli supports a wide variety of output formats, like `table`, `csv`, `json`, `xml`, and `tree` formats, allowing for both human-readable output or output suitable for automations. The `csv-plain` and `table-plain` output formats produce CSV or table output without headers. The `*-flat` output formats produce a flattened view of the output data, potentially making it easier to process that data without having to navigate through an object tree. For a full list of output formats supported by your fcli version, please refer to the help output or [Manual Pages](#manual-pages).

Most output formats allow for specifying the JSON properties to be included in the output, for example `-o csv=id,name`. If no JSON properties are specified, most output formats will output all available JSON properties, except for table output, which usually outputs a predefined set of JSON properties.

There are two output formats that are somewhat special:
* `-o 'expr=Text with {property1} or {property2}\n'` 
     Formats the output data based on the given expression, which is a combination of (optional) plain text and JSON property placeholders. This can be used for a variety of purposes, for example generating output in a human-readable format, or for generating a list of commands to be run at a later stage. Note that by default, no newline character will be inserted after evaluating the given expression. If necessary, the expression should explicitly include `\n` to output a newline character. To demonstrate the power of this output format, following are two examples of how `-o expr` can be used to generate a script that purges all application versions matching certain criteria: 
     * `fcli ssc appversion list -q createdBy=admin -o 'expr=fcli ssc appversion-artifact purge --older-than 30d --appversion {id}\n'`
     * for id in $(fcli ssc appversion list -q createdBy=admin -o 'expr={id}\n'); do echo "fcli ssc appversion-artifact purge --older-than 30d --appversion ${id}"; done
* `-o json-properties` 
     List all JSON properties returned by the current command, which can be used on options that take JSON properties as input, like output expressions (`-o expr={prop}`), properties to include in the output (`-o table=prop1,prop2`), queries (`-q prop1=value1`), and fcli variables (`--store var:prop1,prop2` & `{?var:prop1}`). Two important notes about this output format:
     * The command will be executed as specified, so be careful when using this output option on any command that changes state (delete/update/create/...)
     * On some commands, the list of available JSON properties may vary depending on command line options. For example, when a query returns no records, then `-o json-properties` will not output any properties. Likewise, a command may provide options for including additional data for each record; the corresponding JSON properties will only be shown if `-o json-properties` is used in combination with these options that load additional data.

### --output-to-file
Available on virtually all (leaf) commands that output data, this option can be used to write the command output data to a file, in the format specified by the `--output` option listed above. In some cases, this may be more convenient than redirecting the output to a file. For example, although currently not implemented, fcli could potentially skip creating the output file if there is no output data or if an error occurs. Also, for commands that output status updates, like `wait-for` commands, the `--output-to-file` option allows for status updates to be written to standard output while the final output of the command will be written to the file specified.

### --store
Available on virtually all (leaf) commands that output data, this option can be used to store command output data in an fcli variable. For more details, see the [Fcli Variables](#fcli-variables) section.

### -q | --query
Available on most `list` commands and some other commands, this option allows for querying the output data, outputting only records that match the given query or queries. For now, only equals-based matching is supported; future fcli versions may provide additional matching options. General format is `-q <json-property>=<value>`; the list of JSON properties available for matching can be found by executing the same command with the `-o json-properties` option; see [-o | --output](#-o--output) for details.

### --session
Available on virtually all commands that interact with a target system, this option allows for specifying a session name. For more details, see the [Session Management](#session-management) section.

## Session Management
Most fcli product modules are session-based, meaning that you need to run a `session login` command before you can use most of the other commands provided by a product module, and run a `session logout` command when finished, for example:

```bash
fcli ssc session login --url https://my.ssc.org/ssc --user <user> --password <password>
fcli ssc appversion list
fcli ssc session logout --user <user> --password <password>
```

For interactive use, you can choose to keep the session open until it expires (expiration period depends on target system and login method). For pipeline use or other automation scenarios, it is highly recommended to issue a `session logout` command when no further interaction with the target system is required, to allow for any client-side and server-side cleanup to be performed. For example, upon logging in to SSC with user credentials, fcli will generate a `UnifiedLoginToken`, which will be invalidated when the `ssc session logout` is being run. If you have many (frequently executed) pipelines that interact with SSC, and you don't run the `ssc session logout` command when the pipeline finishes, you risk exhausting SSC's limit on active tokens. In addition, the `logout` commands will remove the session details like URL and authentication tokens from the client system, and perform other cleanup like removing predefined fcli variables (see [Fcli Variables](#fcli-variables)).

For product modules that support it, like SSC or ScanCentral DAST, it is also highly recommended to use token-based authentication rather than username/password-based authentication when incorporating fcli into pipelines or other automation tasks. This will avoid creation of a temporary token as described above, but also allows for better access control based on token permissions. Similarly, for systems that support Personal Access tokens, like FoD, it is highly recommended to utilize a Personal Access Token rather than user password. Note however that depending on (personal access) token permissions, not all fcli functionality may be available. In particular, even the least restrictive SSC `CIToken` may not provide access to all endpoints covered by fcli. If you need access to functionality not covered by `CIToken`, you may need to define a custom token definition, but this can only be done on self-hosted SSC environments, not on Fortify Hosted. If all else fails, you may need to revert to username/password-based authentication to utilize the short-lived `UnifiedLoginToken`.

### Named Sessions
Fcli supports named sessions, allowing you to have multiple sessions for a single product.

TODO

### Session Storage
To keep session state between fcli invocations, fcli stores session data like URL and authentication tokens in the [Fcli Home Folder](#fcli-home-folder). To reduce the risk of unauthorized access to this sensitive data, fcli encrypts the session data files. However, this is not bullet-proof, as the encryption key and algorithm can be easily viewed in fcli source code. As such, it is recommended to ensure file permissions on the FCLI Home folder are properly configured to disallow access by other users. Being stored in the user's home directory by default, the correct file permissions should usually already be in place.

Future fcli versions may provide enhancements to further improve protection of this sensitive data, for example by:
- Allowing the user to specify a custom encryption password through an environment variable, such that the session data can only be decrypted if the environment variable value is known
- Provide functionality for running multiple fcli commands with a single fcli invocation, like providing an `fcli shell` command or running commands from an fcli workflow definition file, which should allow session data data to be stored in memory instead of on disk, and also allow for automated logout when exiting the fcli shell or when the workflow finishes


## Fcli Home Folder
Fcli stores various files in its home directory, like session files (see [Session Management](#session-management)) and fcli variable contents (see [Fcli Variables](#fcli-variables)). Future versions of fcli may also automatically generated log files in this home directory, if no `--log-file` option is provided. 

By default, the fcli home directory is located at `<user home directory>/.fortify/fcli`, but this can be overridden through the `FORTIFY_HOME` or `FCLI_HOME` environment variables. If the `FCLI_HOME` environment variable is set, then this will be used as the fcli home directory. If the `FORTIFY_HOME` environment variable is set (and `FCLI_HOME` is not set), then fcli will use `<FORTIFY_HOME>/fcli` as its home directory.

TODO pipeline use

## Environment Variables

TODO

## Fcli Variables

TODO

## Manual Pages
Manual pages for individual fcli releases can be downloaded from the Assets sections on the [fcli releases page](https://github.com/fortify-ps/fcli/releases), or can be viewed online here. Online manual pages are available for the following versions:

### Release versions
{% assign manpages_release = site.static_files | where: "manpage_release", true %}
{% assign has_releaseDocs = manpages_release | first %}
{% if has_releaseDocs %}
- [ {{ page.path | replace_first: "/", "" | split: "/" | first }} ]( /fcli{{ page.path }} )
{% else %}
- *No release version documentation available*
{% endif %}

### Development versions (latest builds)
Note that development versions may be updated at any time. The manual pages listed here are based on the latest build of a particular development version, and may not match if you are running an older build of a development version of fcli.

{% assign manpages_dev = site.static_files | where: "manpage_dev", true %}
{% assign has_devDocs = manpages_dev | first %}
{% if has_devDocs %}
{% for page in manpages_dev %}
- [ {{ page.path | replace_first: "/", "" | split: "/" | first }} ]( /fcli{{ page.path }} )
{% endfor %}
{% else %}
- *No development version documentation available*
{% endif %}

## Troubleshooting

TODO

<!-- Empty space to allow scrolling to all sections when selecting a section in the navigation bar -->
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

