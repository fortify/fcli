# Fortify CLI (fcli) Installation & Usage
The fcli utility can be used to interact with various Fortify products, like FoD, SSC, ScanCentral SAST and ScanCentral DAST. This document describes installation and general usage of fcli. For a full listing of fcli commands and corresponding command line options, please see the man-pages corresponding to the fcli version that you are using, as listed in the [fcli introduction]({% post_url 2022-06-01-intro %}).

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

## General Command Structure

Fcli is modeled after Fortify products.

TODO

## Sessions

TODO

## Environment Variables

TODO

## Fcli Variables

TODO