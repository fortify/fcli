# Changelog

## [2.5.2](https://github.com/fortify/fcli/compare/v2.5.1...v2.5.2) (2024-08-21)


### Bug Fixes

* FoD/SSC: Improve `github-pr-comment` action output ([694e7ae](https://github.com/fortify/fcli/commit/694e7aec4d9a47b8219629cdf4332b227c1a87a8))
* SSC: Fix application version link in `appversion-summary` & `bitbucket-sast-report` actions ([4f40a04](https://github.com/fortify/fcli/commit/4f40a04eb442ca2872799f182107a8ed593443e1))

## [2.5.1](https://github.com/fortify/fcli/compare/v2.5.0...v2.5.1) (2024-08-14)


### Bug Fixes

* `fcli fod mast-scan start`: Add `--platform` option as required by current FoD API ([7703939](https://github.com/fortify/fcli/commit/7703939be19eca56855c12153c0be25962af29b8))
* `fcli fod mast-scan start`: Fix description for `--file` option ([7703939](https://github.com/fortify/fcli/commit/7703939be19eca56855c12153c0be25962af29b8))

## [2.5.0](https://github.com/fortify/fcli/compare/v2.4.0...v2.5.0) (2024-08-13)


### Features

* `fcli ssc appversion create`: Allow for copying attributes & user access ([667ba4f](https://github.com/fortify/fcli/commit/667ba4f08ba1bed9fb32f9d2c3bd9fb376a1c154))
* FoD: Debricked SBOM Export/Import (resolves [#560](https://github.com/fortify/fcli/issues/560)) ([aac8e10](https://github.com/fortify/fcli/commit/aac8e10661e141a72caa4c948bc6a980033d62fe))


### Bug Fixes

* `fcli fod issue list`: Add `--include` option to allow for retrieving `fixed` and/or `suppressed` issues (fixes [#545](https://github.com/fortify/fcli/issues/545)) ([01c2ac2](https://github.com/fortify/fcli/commit/01c2ac2e2110ac53aa2d75c8047c60eda6bc8e2a))
* `fcli ssc issue list`: Add `--include` option to allow for retrieving `hidden`, `fixed` and/or `suppressed` issues ([318ca98](https://github.com/fortify/fcli/commit/318ca981b5bb0de685192e11e24dbe017186bfd6))
* fcli fod action run release-summary fails parsing scan dates (fixes fortify[#569](https://github.com/fortify/fcli/issues/569)) ([#570](https://github.com/fortify/fcli/issues/570)) ([9ed8032](https://github.com/fortify/fcli/commit/9ed8032305285bcc38a658b35ba1f288c52b476c))
* Fix exception in `github-sast-report` & `sarif-sast-report` actions if there are no SAST issues to be processed ([01bce49](https://github.com/fortify/fcli/commit/01bce4931f4e235340f3a02763b2e486002dedcb))
* No longer require user credentials on SSC, SC-SAST & SC-DAST logout commands (requires SSC 24.2+) ([cb7867b](https://github.com/fortify/fcli/commit/cb7867bccb9629ccf9614d4448f70e48484c7503))
* NullPointerException in `fcli fod *ast-scan get` (fixes [#553](https://github.com/fortify/fcli/issues/553)) ([f2eab9c](https://github.com/fortify/fcli/commit/f2eab9cfd20dd0c51201106152a981058962d207))
* Pass non-default session name to fcli: action statements (fixes [#555](https://github.com/fortify/fcli/issues/555)) ([8b762e2](https://github.com/fortify/fcli/commit/8b762e2f11187aca6ba7245f02e13629e3fc7632))
* Update copyright statement to 2024 ([833c607](https://github.com/fortify/fcli/commit/833c6079cd87658232e4a6edac8fff5e33dfe2b6))
* Update release-summary action to include OSS (resolves [#561](https://github.com/fortify/fcli/issues/561)) ([aac8e10](https://github.com/fortify/fcli/commit/aac8e10661e141a72caa4c948bc6a980033d62fe))
* When authenticating with an SSC authentication token, the SSC, SC-SAST & SC-DAST session commands will now display token expiration date (requires SSC 24.2+) ([c2e66bc](https://github.com/fortify/fcli/commit/c2e66bceb34ca1c3f9989dd2c63546ff0c010d99))
* When authenticating with an SSC authentication token, the SSC, SC-SAST & SC-DAST session login commands will now validate whether the given token is a valid token ([c2e66bc](https://github.com/fortify/fcli/commit/c2e66bceb34ca1c3f9989dd2c63546ff0c010d99))

## [2.4.0](https://github.com/fortify/fcli/compare/v2.3.0...v2.4.0) (2024-05-17)


### Features

* Add `fcli config public-key` commands for managing trusted public keys ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add `fcli fod action` commands for running a variety of yaml-based actions ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add `fcli fod issue list` command ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add `fcli ssc action` commands for running a variety of yaml-based actions ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add `fcli ssc issue list` command ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add actions for generating application version/release summary ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add actions for generating BitBucket, GitHub, GitLab, SARIF and SonarQube vulnerability reports ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add preview actions for generating GitHub Pull Request comments ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Add sample actions for checking security policy criteria ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* Migrate FortifyVulnerabilityExporter functionality to yaml-based fcli actions ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))


### Bug Fixes

* `fcli ssc appversion create`: Command will now fail instead of creating uncommitted application version if the application version specified on `--copy-from` option does not exist ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))
* FoD: Update `wait-for` commands to use internal API (closes [#526](https://github.com/fortify/fcli/issues/526), [#500](https://github.com/fortify/fcli/issues/500)) ([4dff325](https://github.com/fortify/fcli/commit/4dff325bd52b27fad55e417b82af3bc00b8d756a))

## [2.3.0](https://github.com/fortify/fcli/compare/v2.2.0...v2.3.0) (2024-03-05)


### Features

* Add support for configuring proxy settings through conventional environment variables HTTP_PROXY, HTTPS_PROXY, ALL_PROXY & NO_PROXY (used if proxy is not explicitly configured through 'fcli config proxy' commands) ([881adbd](https://github.com/fortify/fcli/commit/881adbda905d83d61045c01f706633691f19496e))

## [2.2.0](https://github.com/fortify/fcli/compare/v2.1.0...v2.2.0) (2024-02-05)


### Features

* `fcli fod`: Add `fcli fod report` commands for creating and downloading FoD reports (resolves [#263](https://github.com/fortify/fcli/issues/263)) ([5796379](https://github.com/fortify/fcli/commit/579637905499e75e33eff0317d5d52c246802326))
* `fcli fod`: Add preview commands for starting and managing DAST Automated scans ([db898ee](https://github.com/fortify/fcli/commit/db898ee39453c68c88c18e9134278635782f31cb))
* `fcli ssc`: Add `fcli ssc report` commands for generating, downloading & managing SSC reports (resolves [#205](https://github.com/fortify/fcli/issues/205)) ([60e7855](https://github.com/fortify/fcli/commit/60e78551cf14fd6644484eb1bc2e9340abf6231d))
* `fcli tool`: Add `fcli tool * install --base-dir` option to specify the base directory under which all tools will be installed. By default, fcli will now also install tool invocation scripts in a global `<base-dir>/bin` directory, unless the `--no-global-bin` option is specified. This allows for having a single bin-directory on the `PATH`, while managing the actual tool versions being invoked through the `fcli tool * install` commands. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: Add `fcli tool * install --uninstall` option to remove existing tool installations while installing a new tool version, allowing for easy tool upgrades. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: Add `fcli tool debricked-cli` commands for installing Debricked CLI and managing those installations. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: Add `fcli tool definitions` commands, allowing tool definitions to be updated to make fcli aware of new tool versions that were released after the current fcli release. Customers may also host customized tool definitions, for example allowing for alternative tool download URLs or restricting the set of tool versions available to end users. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: Add `fcli tool fcli` commands for installing Fortify CLI and managing those installations. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: By default, the `fcli tool * install` commands will now install tools under the `<user.home>/fortify/tools` base directory (no dot/hidden directory), instead of `<user.home>/.fortify/tools` ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))
* `fcli tool`: Deprecate `fcli tool * install --install-dir` option; the new `--base-dir` option is now preferred as it supports new functionality like global bin-scripts. ([e2db51d](https://github.com/fortify/fcli/commit/e2db51d05567f9c7fcaa0bd96548b93fd69fea8a))


### Bug Fixes

* `fcli ssc`: The `--attributes` option on `fcli ssc appversion *` and `fcli ssc attribute *` commands now supports setting multiple values for an attribute ([bd3fd62](https://github.com/fortify/fcli/commit/bd3fd625125b410ac321ef08d7418b26d0643a58))

## [2.1.0](https://github.com/fortify/fcli/compare/v2.0.0...v2.1.0) (2023-11-21)


### Features

* `fcli ssc appversion create`: Add options for copying existing application version ([75461db](https://github.com/fortify/fcli/commit/75461db9be93425365fff9e07046ae074da36241))
* Add `fcli ssc appversion copy-state` command ([75461db](https://github.com/fortify/fcli/commit/75461db9be93425365fff9e07046ae074da36241))
* Add `fcli system-state wait-for-job` command ([75461db](https://github.com/fortify/fcli/commit/75461db9be93425365fff9e07046ae074da36241))


### Bug Fixes

* rename new SSC_URL `PROJECT_VERSION_ACTION`-&gt; `PROJECT_VERSIONS_ACTION` ([55178be](https://github.com/fortify/fcli/commit/55178be0d90a6e42e9ccf7f5acd9a632492b1e1a))

## [2.0.0](https://github.com/fortify/fcli/compare/v1.3.2...v2.0.0) (2023-10-25)


### ⚠ BREAKING CHANGES

* Core: Most commands/options now use case-sensitive matching to avoid inconsistent behavior between server-side and client-side matching
* Core: Change fcli variable syntax & behavior for easier use
* Core: Change query expression syntax to allow for advanced queries
* Core: Restructure fcli home/data directories. Configuration & session data stored by earlier fcli versions will not be available after upgrading, and will not be automatically removed. It's recommended to manually delete the `~/.fortify/fcli` folder when upgrading, and then use the new fcli version to re-apply configuration settings.
* Core: Change environment variable names for better clarity and avoiding conflicts with other Fortify command-line utilities
* Core: The .jar version of fcli now requires Java 17 or higher to run
* `fcli config`: Restructure command tree & options for consistency & ease of use
* `fcli config`: Move variable-related commands to `fcli util`
* `fcli fod`: Restructure existing commands & options for consistency & ease of use
* `fcli sc-dast`: Minor restructuring of command tree & options for consistency & ease of use
* `fcli sc-sast`: Minor restructuring of command options for consistency & ease of use
* `fcli ssc`: Restructure existing commands & options for consistency & ease of use
* `fcli tool`: Minor restructuring of command options for consistency & ease of use

### Features

* `fcli config`: Move variable-related commands to `fcli util` ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli config`: Restructure command tree & options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli fod`: Fixes, usability improvements & new commands for managing applications, microservices, releases, scans & scan results ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli fod`: Move out of preview mode, now officially supported ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli fod`: Restructure existing commands & options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli fod`: Various other fixes & usability improvements ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli license`: New command, adding support for generating MSP & NCD license usage reports ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli sc-dast`: Minor restructuring of command tree & options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli sc-dast`: Various fixes & usability improvements ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli sc-sast`: Minor restructuring of command options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli sc-sast`: New command for listing ScanCentral SAST sensors ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli sc-sast`: Various fixes & usability improvements ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: Add support for applying filters on issue counts ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: Add support for embedding additional data on `fcli ssc appversion get/list` commands ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: New commands for creating local users, refreshing metrics, listing rule packs & listing SSC configuration settings ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: New commands for managing performance indicators & variables (PREVIEW) ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: Restructure existing commands & options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli ssc`: Various other fixes & usability improvements ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli tool`: Add support for FortifyBugTrackerUtility ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli tool`: Improve tool version & digest handling ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli tool`: Minor restructuring of command options for consistency & ease of use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli util`: Add variable-related commands (moved from `fcli config`) ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* `fcli util`: Add various other utility commands ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Add support for interactive confirmation on commands that require confirmation ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Change environment variable names for better clarity and avoiding conflicts with other Fortify command-line utilities ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Change fcli variable syntax & behavior for easier use ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Change query expression syntax to allow for advanced queries ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Restructure fcli home/data directories. Configuration & session data stored by earlier fcli versions will not be available after upgrading, and will not be automatically removed. It's recommended to manually delete the `~/.fortify/fcli` folder when upgrading, and then use the new fcli version to re-apply configuration settings. ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: The .jar version of fcli now requires Java 17 or higher to run ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))


### Bug Fixes

* Core: Most commands/options now use case-sensitive matching to avoid inconsistent behavior between server-side and client-side matching ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))
* Core: Various bug fixes and many other improvements ([ae7ad75](https://github.com/fortify/fcli/commit/ae7ad75a1572cd1933334821730a4ea4e471f03b))

## [1.3.2](https://github.com/fortify/fcli/compare/v1.3.1...v1.3.2) (2023-10-12)


### Bug Fixes

* `fcli tool vuln-exporter install`: Add support for latest (2.0.4) version ([a44ddc3](https://github.com/fortify/fcli/commit/a44ddc3d74b1a2ad92a2fb41c65a0c7c5dbcc0aa))

## [1.3.1](https://github.com/fortify/fcli/compare/v1.3.0...v1.3.1) (2023-09-20)


### Bug Fixes

* `fcli tool sc-client install`: Add support for latest (23.1.0) version ([93af1c6](https://github.com/fortify/fcli/commit/93af1c62a70eaa7fcd1867d59d7a92a6de256f25))
* `fcli tool vuln-exporter install`: Add support for latest (2.0.3) version ([c7d4af6](https://github.com/fortify/fcli/commit/c7d4af604c5f13aecbdd4271d2fb6f04a2c9f369))

## [1.3.0](https://github.com/fortify/fcli/compare/v1.2.5...v1.3.0) (2023-08-18)


### Features

* Configurable connect & socket timeout ([3015bb5](https://github.com/fortify/fcli/commit/3015bb5c7d1f36cb5aa6e55b32319dc58ebed0aa))

## [1.2.5](https://github.com/fortify/fcli/compare/v1.2.4...v1.2.5) (2023-04-07)


### Bug Fixes

* `fcli tool vuln-exporter install`: Add support for latest (2.0.2) version ([e0ce21a](https://github.com/fortify/fcli/commit/e0ce21a851d4f5f85b6ea34cbcbb8a8d18cdff2c))

## [1.2.4](https://github.com/fortify/fcli/compare/v1.2.2...v1.2.4) (2023-04-07)


### Bug Fixes

* `fcli tool vuln-exporter install`: Add support for latest (2.0.1) version ([9c34f73](https://github.com/fortify/fcli/commit/9c34f73eb4b7b5474e742d138b908cff6042f438))


## [1.2.3](https://github.com/fortify/fcli/compare/v1.2.2...v1.2.3) (2023-03-09)


### Bug Fixes

* `fcli ssc appversion-artifact download`: Include externalmetadata.xml in current state FPR download by passing arbitrary clientVersion parameter to SSC (fixes [#257](https://github.com/fortify/fcli/issues/257)) ([2694ffe](https://github.com/fortify/fcli/commit/2694ffe0224d85121ea0eaadda64464a0f6f3ff5))

### [1.2.2](https://www.github.com/fortify-ps/fcli/compare/v1.2.1...v1.2.2) (2023-03-05)


### Bug Fixes

* `fcli tool sc-client install`: Add support for latest (22.2.1) version ([38e93eb](https://www.github.com/fortify-ps/fcli/commit/38e93eb590c15b26090f8b0ae29c761a72db5269))

### [1.2.1](https://www.github.com/fortify-ps/fcli/compare/v1.2.0...v1.2.1) (2023-03-05)


### Bug Fixes

* Custom trust store ignored by native binaries (fixes [#253](https://www.github.com/fortify-ps/fcli/issues/253)) ([a0af875](https://www.github.com/fortify-ps/fcli/commit/a0af875a2bd511b75863c1c15c8ea1a089e1b4f2))

## [1.2.0](https://www.github.com/fortify-ps/fcli/compare/v1.1.0...v1.2.0) (2023-02-09)


### Features

* FoD: Add `fod sast-scan setup` (implements [#225](https://www.github.com/fortify-ps/fcli/issues/225)) ([e556f1e](https://www.github.com/fortify-ps/fcli/commit/e556f1e027f8adb5f164fc4e67af163e83e6fd6e))
* FoD: Added functionality for user CRUD (implements [#245](https://www.github.com/fortify-ps/fcli/issues/245)) ([818622a](https://www.github.com/fortify-ps/fcli/commit/818622acc3050ea9289a45739ef6dffc9832073e))
* FoD: Added functionality for user group CRUD (implements [#246](https://www.github.com/fortify-ps/fcli/issues/246)) ([818622a](https://www.github.com/fortify-ps/fcli/commit/818622acc3050ea9289a45739ef6dffc9832073e))


### Bug Fixes

* `fcli tool vuln-exporter install`: Add support for latest (2.0.0) version ([d7ccaea](https://www.github.com/fortify-ps/fcli/commit/d7ccaea378256d7807020b96499e47bad8aadf3e))

## [1.1.0](https://www.github.com/fortify-ps/fcli/compare/v1.0.5...v1.1.0) (2023-01-19)


### Features

* Add support for configuring custom SSL trust store (fixes [#221](https://www.github.com/fortify-ps/fcli/issues/221)) ([2732e37](https://www.github.com/fortify-ps/fcli/commit/2732e3710c7fb9e2eff583049608d132f7bc0cfa))
* SSC: Add support for importing Debricked results ([e2a6f1e](https://www.github.com/fortify-ps/fcli/commit/e2a6f1e552657cdb485f2bd998233d0641212210))


### Bug Fixes

* `fcli * session login`: Improve error output on previous session logout failure (fixes [#219](https://www.github.com/fortify-ps/fcli/issues/219)) ([86b0868](https://www.github.com/fortify-ps/fcli/commit/86b08688860507623029bf4f12e68116d88d2417))
* `fcli sc-dast session login`: Require SSC credentials to be specified (fixes [#223](https://www.github.com/fortify-ps/fcli/issues/223)) ([ea049ec](https://www.github.com/fortify-ps/fcli/commit/ea049ec17ecc17388c425cff588be22c47be91ed))
* `fcli sc-sast scan start`: `NullPointerException` instead of proper error message if no options provided (fixes [#232](https://www.github.com/fortify-ps/fcli/issues/232)) ([1efa62b](https://www.github.com/fortify-ps/fcli/commit/1efa62b458c1352140cc497888da9b2339f55a08))
* `fcli sc-sast session login`: Improve usage help for `--client-auth-token` and explicitly check token validity (fixes [#230](https://www.github.com/fortify-ps/fcli/issues/230)) ([ce6324b](https://www.github.com/fortify-ps/fcli/commit/ce6324b10c110297aaecefee5abdd0c41cee6172))
* `fcli sc-sast session login`: Require SSC credentials to be specified (fixes [#222](https://www.github.com/fortify-ps/fcli/issues/222)) ([b252069](https://www.github.com/fortify-ps/fcli/commit/b252069b208442745399c376d53612fe857e44df))
* Fix NoSuchFileExceptions if FCLI_HOME or FORTIFY_HOME set to relative directory (fixes [#227](https://www.github.com/fortify-ps/fcli/issues/227)) ([2ef6b21](https://www.github.com/fortify-ps/fcli/commit/2ef6b2134fe69b2706a4c0742bb9008feb16b68b))
* Fix NullPointerException if no module(s) configured for proxy (fixes [#228](https://www.github.com/fortify-ps/fcli/issues/228)) ([11ec6e1](https://www.github.com/fortify-ps/fcli/commit/11ec6e18c934d7f9dbd3b983297a4d17c0f9f650))
* Improve help output for `-h` option (fixes [#217](https://www.github.com/fortify-ps/fcli/issues/217)) ([f2e47b0](https://www.github.com/fortify-ps/fcli/commit/f2e47b024f384f5fdb60a949613bf299bfd4f515))
* Improve output of session commands to provide better consistency with other CRUD commands (fixes [#220](https://www.github.com/fortify-ps/fcli/issues/220)) ([153f96e](https://www.github.com/fortify-ps/fcli/commit/153f96efc202aea209a5ac961886a21ec21cd901))
* SSL verification was incorrectly disabled by default and enabled by `-k` option (fixes [#231](https://www.github.com/fortify-ps/fcli/issues/231)) ([7fa56c3](https://www.github.com/fortify-ps/fcli/commit/7fa56c31caa13fee9715662dd9b44a972cfda39e))

### [1.0.5](https://www.github.com/fortify-ps/fcli/compare/v1.0.4...v1.0.5) (2023-01-11)


### Bug Fixes

* FoD: Fix some commands not working in native binaries ([#216](https://www.github.com/fortify-ps/fcli/issues/216)) ([02baa48](https://www.github.com/fortify-ps/fcli/commit/02baa4862035e7cf027cfbec9a79545a29fe9a5c))

### [1.0.4](https://www.github.com/fortify-ps/fcli/compare/v1.0.3...v1.0.4) (2023-01-03)


### Bug Fixes

* `fcli sc-sast scan start`: Accept both encoded or decoded token for `--ssc-ci-token` option (fixes [#215](https://www.github.com/fortify-ps/fcli/issues/215)) ([1c0ba17](https://www.github.com/fortify-ps/fcli/commit/1c0ba17765b0c651381398a23e21607e87606e92))
* Improve interactive prompts (fixes [#213](https://www.github.com/fortify-ps/fcli/issues/213)) ([ad15067](https://www.github.com/fortify-ps/fcli/commit/ad15067bd01260c18ec8c6f5ac5244b2087f753d))

### [1.0.3](https://www.github.com/fortify-ps/fcli/compare/v1.0.2...v1.0.3) (2022-12-22)


### Bug Fixes

* `fcli config var def list`: Show created date as last accessed date if variable contents haven't been read yet (fixes [#207](https://www.github.com/fortify-ps/fcli/issues/207)) ([302c9ca](https://www.github.com/fortify-ps/fcli/commit/302c9ca3d51ad2e3699ccbca2013d7c273462296))
* `fcli sc-dast sensor enable/disable`: Fix HostNotFoundException due to hidden non-ASCII characters in endpoint URI (fixes [#212](https://www.github.com/fortify-ps/fcli/issues/212)) ([ca65080](https://www.github.com/fortify-ps/fcli/commit/ca65080327f8251d3ba0a2aad3a89c03e6fd4e7c))
* `fcli ssc appversion-vuln count`: Add missing `-q` option (fixes [#209](https://www.github.com/fortify-ps/fcli/issues/209)) ([cdb2849](https://www.github.com/fortify-ps/fcli/commit/cdb28495ff12b817ee735945bebc624564d77b2d))
* Better description of default behavior for boolean options (fixes [#206](https://www.github.com/fortify-ps/fcli/issues/206)) ([903c1c4](https://www.github.com/fortify-ps/fcli/commit/903c1c45126fb59b5d599d0155eff518400f160f))
* Fix ANSI (color) codes on Windows ([05e159e](https://www.github.com/fortify-ps/fcli/commit/05e159e1fe107956bfedd556383bad3f3904f4c7))

### [1.0.2](https://www.github.com/fortify-ps/fcli/compare/v1.0.1...v1.0.2) (2022-12-16)


### Bug Fixes

* Fix `fcli --version` not displaying version number in native binaries (fixes [#112](https://www.github.com/fortify-ps/fcli/issues/112)) ([b3b48e6](https://www.github.com/fortify-ps/fcli/commit/b3b48e6ed49d3a138138383769a127b4ee0b8998))

### [1.0.1](https://www.github.com/fortify-ps/fcli/compare/v1.0.0...v1.0.1) (2022-12-15)


### Bug Fixes

* `fcli ssc app update`: Fix 'application not found' error when updating app name (fixes [#166](https://www.github.com/fortify-ps/fcli/issues/166)) ([f8ebad6](https://www.github.com/fortify-ps/fcli/commit/f8ebad68a1ce3c788fd2165b8b30e3540dd65242))
* `fcli ssc appversion update`: Fix application name not shown in output (fixes [#183](https://www.github.com/fortify-ps/fcli/issues/183)) ([32f130b](https://www.github.com/fortify-ps/fcli/commit/32f130b1a5448a89e55fef7e40dbbc23d0573323))
* `fcli ssc appversion update`: Fix exception if no --userdel option is specified (fixes [#175](https://www.github.com/fortify-ps/fcli/issues/175)) ([c7ebb98](https://www.github.com/fortify-ps/fcli/commit/c7ebb98dbdf1cd3795921d9229b8c0a53df71bbd))
* `fcli ssc appversion-artifact download`: `--no-include-sources` now available for both application file and individual FPR download (fixes [#173](https://www.github.com/fortify-ps/fcli/issues/173)) ([216ac2a](https://www.github.com/fortify-ps/fcli/commit/216ac2a61ea1b6722462d279923ea6f4bc744d5d))
* `fcli ssc appversion-artifact download`: HTTP 500 error when downloading application file ([216ac2a](https://www.github.com/fortify-ps/fcli/commit/216ac2a61ea1b6722462d279923ea6f4bc744d5d))
* `fcli ssc appversion-artifact upload`: Improve usage message for `--engine-type` option (fixes [#176](https://www.github.com/fortify-ps/fcli/issues/176)) ([6cc775e](https://www.github.com/fortify-ps/fcli/commit/6cc775ebf4a75e37893ea16c7bb7752d3a3a8d83))
* `fcli ssc attribute-definition get`: Allow category prefix when specifying guid (fixes [#186](https://www.github.com/fortify-ps/fcli/issues/186)) ([7b02f61](https://www.github.com/fortify-ps/fcli/commit/7b02f61fd7d944fc08ee9d024652c54e65d5712b))
* `fcli ssc issue-template create`: Display 'Default template=true' if `--set-as-default` specified (fixes [#180](https://www.github.com/fortify-ps/fcli/issues/180)) ([6f2101e](https://www.github.com/fortify-ps/fcli/commit/6f2101ee6aa6333ff0b5553c11ef9656973a6cc6))
* `fcli ssc issue-template delete`: Fix issue templates not being deleted (fixes [#182](https://www.github.com/fortify-ps/fcli/issues/182)) ([0b55974](https://www.github.com/fortify-ps/fcli/commit/0b559746043eb0086c14d87f2c0013c225cf99d7))
* `fcli ssc issue-template update`: Fix 'issue template not found' error when updating issue template name (fixes [#181](https://www.github.com/fortify-ps/fcli/issues/181)) ([a6002b1](https://www.github.com/fortify-ps/fcli/commit/a6002b12975b2c3858313f83c001f962a9626b6c))
* `fcli ssc plugin`: Fix "No serializer" errors (fixes [#187](https://www.github.com/fortify-ps/fcli/issues/187), fixes [#188](https://www.github.com/fortify-ps/fcli/issues/188)) ([88d8886](https://www.github.com/fortify-ps/fcli/commit/88d88867439754acf86abd302410dd494e28937e))
* `fcli ssc role create`: Allow comma-separated list of permission id's (fixes [#190](https://www.github.com/fortify-ps/fcli/issues/190)) ([1426116](https://www.github.com/fortify-ps/fcli/commit/1426116932dc4098ac68055498c505098682ea3b))
* `fcli ssc role delete`: Fix role not being deleted (fixes [#191](https://www.github.com/fortify-ps/fcli/issues/191)) ([e329c89](https://www.github.com/fortify-ps/fcli/commit/e329c891dbdf2136f0164f04d6440914940a6e14))
* `fcli ssc token update`: Improve usage message (fixes [#177](https://www.github.com/fortify-ps/fcli/issues/177)) ([8e8b924](https://www.github.com/fortify-ps/fcli/commit/8e8b9243b591c5104117faf11ad75b70c4e6deba))
* `fcli ssc token`: Make output more consistent with SSC UI (fixes [#194](https://www.github.com/fortify-ps/fcli/issues/194)) ([35523cc](https://www.github.com/fortify-ps/fcli/commit/35523cc2066c289802d2dc4bda7f4c01cbcbe554))
* `fcli tool sc-client install`: Add support for latest (22.2.0) version (fixes [#179](https://www.github.com/fortify-ps/fcli/issues/179)) ([dac4b37](https://www.github.com/fortify-ps/fcli/commit/dac4b373571b11c81f48e9319dc889a911e22704))

## 1.0.0 (2022-11-29)


### Miscellaneous Chores

* release 1.0.0 ([d983f62](https://www.github.com/fortify-ps/fcli/commit/d983f62c01d38ca5cef8963f9ce98c7a2d19c0ab))
