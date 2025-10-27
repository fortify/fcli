# Changelog

## [3.11.0](https://github.com/fortify/fcli/compare/v3.10.0...v3.11.0) (2025-10-21)


### Features

* `--style` option: Add `(no-)fast-output` support to enable faster table output by sampling first 100 records to determine column width ([b1e5659](https://github.com/fortify/fcli/commit/b1e56591129a13e7a19c38f35d18b25ce59bd8f0))
* `--style` option: Add `(no-)wrap` support to control text wrapping in table cells ([b1e5659](https://github.com/fortify/fcli/commit/b1e56591129a13e7a19c38f35d18b25ce59bd8f0))
* `ci` action: Detect data from local Git repository ([a52f0ef](https://github.com/fortify/fcli/commit/a52f0ef8a8f1c47815a1462f47c31c19acd2dba0))
* `fcli fod issue ls`: Add `--app` option to allow for listing issues at application level (closes [#596](https://github.com/fortify/fcli/issues/596)) ([f9454b1](https://github.com/fortify/fcli/commit/f9454b14817cc9fb1ec7ca4744551bb740ec700b))
* `fcli ssc issue list`: Add (queryable) `folderName` property in output ([2212093](https://github.com/fortify/fcli/commit/221209366355c6999382b3ad70d05e97dc666afd))
* `fcli ssc rest call`: Add `/api/v1` prefix if missing, to support same endpoint format as listed in the SSC REST API reference in recent SSC versions ([c6dd23b](https://github.com/fortify/fcli/commit/c6dd23b2580f856273efcaf03bb794a93ac5bcb5))
* Action schema: Add `#ifBlank` SpEL function ([a52f0ef](https://github.com/fortify/fcli/commit/a52f0ef8a8f1c47815a1462f47c31c19acd2dba0))
* Action schema: Add `#localRepo` SpEL function for obtaining local repository metadata ([bfeee2e](https://github.com/fortify/fcli/commit/bfeee2e3e8ff998a91bed6d7d1483a55a52e36f8))
* Add `fcli fod attribute` commands for managing FoD attributes (resolves [#679](https://github.com/fortify/fcli/issues/679)) ([3157d64](https://github.com/fortify/fcli/commit/3157d64b5c9ea5654dde1548dcbe8720fe352561))


### Bug Fixes

* `ci` action: Improve Azure DevOps detection logic ([a52f0ef](https://github.com/fortify/fcli/commit/a52f0ef8a8f1c47815a1462f47c31c19acd2dba0))
* `fcli aviator ssc prepare`: Fix exception in fcli native binaries (fixes [#830](https://github.com/fortify/fcli/issues/830)) ([cbbdc2e](https://github.com/fortify/fcli/commit/cbbdc2ead00069b21ef52b1ccd5385e80159d015))
* `fcli aviator`: Fix bugs in FPR source processing and remediation generation ([#833](https://github.com/fortify/fcli/issues/833)) ([9512d78](https://github.com/fortify/fcli/commit/9512d7863a90756038667e1332ba64450c31120d))
* `fcli fod issue ls`: Some fixed issues were not properly annotated with `(F)` (fixes [#820](https://github.com/fortify/fcli/issues/820)) ([f9454b1](https://github.com/fortify/fcli/commit/f9454b14817cc9fb1ec7ca4744551bb740ec700b))
* `fcli fod session login`: Fix NullPointerException if protocol is missing in FoD URL (fixes [#827](https://github.com/fortify/fcli/issues/827)) ([244bd94](https://github.com/fortify/fcli/commit/244bd943c6f51725fa7c69349ea9d5bdcefbcef2))
* `fcli ssc session login`: Improve exception if protocol is missing in SSC or SC-SAST Controller URL ([8e89bd8](https://github.com/fortify/fcli/commit/8e89bd87b1112d2ab99368e1792f16a56f419743))
* Compile native binaries in CPU compatibility mode to allow them to run on more CPU architectures ([0217b8f](https://github.com/fortify/fcli/commit/0217b8f5bd9e3e5b5127e4803afc26b10b89132f))
* Improve table output ([f18f2f0](https://github.com/fortify/fcli/commit/f18f2f0434695a366692f6285e129d954a52d2fb))

## [3.10.0](https://github.com/fortify/fcli/compare/v3.9.1...v3.10.0) (2025-10-09)


### Features

* `fcli aviator app`: Show quota in output of the `create`, `get`, `list`, and `update` commands ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* `fcli aviator ssc audit`: Add `--filterset`, `--no-filterset`, and `--folder` options to allow for selecting issues to be audited ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* Add `fcli aviator fod apply-remediations` command to apply Aviator-proposed remediations from an audited FPR file in FoD to a local source code directory ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* Add `fcli aviator ssc apply-remediations` command to apply Aviator-proposed remediations from an audited FPR file in SSC to a local source code directory ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* Add `fcli aviator ssc prepare` command to configure Aviator custom tags on SSC issue templates and application versions ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* Add `fcli fod action run gitlab-debricked-report` ([#818](https://github.com/fortify/fcli/issues/818)) ([2175af6](https://github.com/fortify/fcli/commit/2175af6277f8d2cb55df83dc5e7e76937e850e4a))


### Bug Fixes

* `fcli aviator ssc audit`: Add preflight check for auditable issues ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* `fcli aviator ssc audit`: Improve FPR handling with Zip File System Provider integration and proper resource cleanup ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* `fcli aviator ssc audit`: Improve FPR parsing speed and memory efficiency ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* `fcli aviator ssc audit`: Improve FPR validation for FPRs with DAST-only issues ([4fd5756](https://github.com/fortify/fcli/commit/4fd5756cda3675a5393ebadcaa4d9bdc31a1ad91))
* `fcli aviator ssc audit`: Prevent race condition crash by checking executor state before retrying gRPC stream ([#819](https://github.com/fortify/fcli/issues/819)) ([646a963](https://github.com/fortify/fcli/commit/646a963fad2cc4e69abf86740bc3c777643c35af))
* `fcli util mcp-server start`: Some common actions were exposed as MCP tools on modules that don't support actions through the CLI ([fa49f97](https://github.com/fortify/fcli/commit/fa49f97ddb9c740fee1f3816a37da0eb3732ee1a))

## [3.9.1](https://github.com/fortify/fcli/compare/v3.9.0...v3.9.1) (2025-10-02)


### Bug Fixes

* `fcli aviator ssc audit`: Improve cleanup of temporary files ([6175f54](https://github.com/fortify/fcli/commit/6175f5426f8be139798382f97d1f19d787d27ec4))
* `fcli aviator ssc audit`: Resolve file token leak during Aviator artifact uploads, to avoid SSC errors due to reaching maximum number of allowed file tokens ([6d2e28c](https://github.com/fortify/fcli/commit/6d2e28ccc4b27c7ba33a255ae31f1e5081e38a48))
* `fcli ssc`: Reduce bulk request batch size to prevent potential networking timeouts due to SSC taking too long to process large bulk requests ([eae7948](https://github.com/fortify/fcli/commit/eae7948035f7d60db90c00fb30ad5e83501bc704))
* `fcli util mcp-server start`: Improve usage help ([d4225ce](https://github.com/fortify/fcli/commit/d4225ce0dd7a4cb728988614c34716be706fe231))
* Improve progress message handling ([9af8e67](https://github.com/fortify/fcli/commit/9af8e679c16bc6736b9c3403d3d20e4dd27e1ee6))

## [3.9.0](https://github.com/fortify/fcli/compare/v3.8.1...v3.9.0) (2025-09-29)


### Features

* `fcli fod *-scan setup`: Implement `--skip-if-exists` option for all scan types (resolves [#593](https://github.com/fortify/fcli/issues/593)) ([219c6f6](https://github.com/fortify/fcli/commit/219c6f603f819e6bfd2d246919ac57957fa13e52))
* `fcli fod microservice`: Add `--attrs` and `--auto-required-attrs` options on applicable micro service commands (resolves [#640](https://github.com/fortify/fcli/issues/640)) ([5b7eb7e](https://github.com/fortify/fcli/commit/5b7eb7e1031237773c737cfe76357d4ba0332dc6))
* `fcli ssc appversion create`: Add `--add-tags` and `--rm-tags` options ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* `fcli ssc appversion update`: Add `--add-tags` and `--rm-tags` options ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* `fcli ssc issue-template create`: Add `--add-tags` and `--rm-tags` options ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* `fcli ssc issue-template update`: Add `--add-tags` and `--rm-tags` options ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* Add `fcli ssc custom-tag` commands for creating, listing, and updating custom tags ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* Add `fcli ssc issue-template` commands for managing issue templates, deprecate corresponding `fcli ssc issue *-template(s)` commands ([6c3a5a4](https://github.com/fortify/fcli/commit/6c3a5a41280d404e3f7b227893af29229ae796f0))
* Add `fcli util mcp-server start` command to allow LLMs to interact with Fortify products through fcli ([#806](https://github.com/fortify/fcli/issues/806)) ([92131a6](https://github.com/fortify/fcli/commit/92131a653d416d693c0449931768d3093e1a7f9f))


### Bug Fixes

* `fcli aviator ssc audit`: Prevent command from stalling on errors & other error handling improvements ([#811](https://github.com/fortify/fcli/issues/811)) ([4deff48](https://github.com/fortify/fcli/commit/4deff4843f276f2de6e21b0b35a465f03a2d7773))
* `fcli fod *-scan wait-for`: Add scan queue position (see [#677](https://github.com/fortify/fcli/issues/677)) ([219c6f6](https://github.com/fortify/fcli/commit/219c6f603f819e6bfd2d246919ac57957fa13e52))
* `fcli fod access-control update-user`: Change action field to `REQUESTED` instead of `UPDATED`, as changes may not be applied immediately by FoD ([09e39bf](https://github.com/fortify/fcli/commit/09e39bf1984f19990e1495997adab6705663a1ab))
* `fcli fod dast-scan cancel` not working ([ba59f6f](https://github.com/fortify/fcli/commit/ba59f6f6d358bf7bf64ceb5e663b75314e05d52d))
* `fcli fod dast-scan start`: Implemented DAST Automated scan queuing/cancelling to avoid error if scan already running (fixes [#565](https://github.com/fortify/fcli/issues/565)) ([51aa462](https://github.com/fortify/fcli/commit/51aa462186f4cbc9b322600c3fc423ee534fa5a3))
* `fcli ssc action run ci`: Fix failure when Aviator audit is enabled (fixes [#789](https://github.com/fortify/fcli/issues/789)) ([103263a](https://github.com/fortify/fcli/commit/103263ad1b5cdc017ec59d5fdf62b6427b2dad53))
* Add fcli action SpEL functions documentation ([#791](https://github.com/fortify/fcli/issues/791)) ([daf54a5](https://github.com/fortify/fcli/commit/daf54a5b48898bd916e8b018a8aba5b14571c82e))

## [3.8.1](https://github.com/fortify/fcli/compare/v3.8.0...v3.8.1) (2025-07-25)


### Bug Fixes

* Fix build issue that caused fcli release process to fail ([4d074d3](https://github.com/fortify/fcli/commit/4d074d3f0b74e991c113fe3ca8ab2019a03950c4))

## [3.8.0](https://github.com/fortify/fcli/compare/v3.7.0...v3.8.0) (2025-07-25)


### Features

* `fcli aviator session login`: Validate connection and token ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* `fcli aviator ssc audit`: Generate remediations.xml with code fixes from aviator audit results ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* `fcli aviator`: SAST Aviator 25.3.0 release ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* `gitlab-sast-report` actions: Add trace nodes ([f2df2e4](https://github.com/fortify/fcli/commit/f2df2e470b6d80b2966ad3d74c8781c38999a154))
* Action schema: Support `if:` instruction on individual `with:` elements ([f6f8175](https://github.com/fortify/fcli/commit/f6f8175b0b9139e003d14df7ce5be102b8c9854d))
* Add `gitlab-codequality-report` actions for SSC and FOD (resolves [#733](https://github.com/fortify/fcli/issues/733)) ([8c9b87c](https://github.com/fortify/fcli/commit/8c9b87c7fd046b52f5ebef97c27b7b74c1c37ac5))
* Add action schema documentation (see [#701](https://github.com/fortify/fcli/issues/701)) ([f1acba0](https://github.com/fortify/fcli/commit/f1acba0bf7cd2c56d0f73fc2ebac897c6f3a81d0))
* FoD `setup-release` action: Add `--store` option to store FoD release data in fcli variable ([e325852](https://github.com/fortify/fcli/commit/e3258528b497bfbfd767027f3660a8a32a0e314f))
* SSC `ci` action: Add support for running Aviator audit after scan completion (resolves [#750](https://github.com/fortify/fcli/issues/750)) ([5722a68](https://github.com/fortify/fcli/commit/5722a681ebc71004478a142c43595fa70a27da8b))
* SSC `setup-appversion` action: Add `--store` option to store SSC application version data in fcli variable ([e325852](https://github.com/fortify/fcli/commit/e3258528b497bfbfd767027f3660a8a32a0e314f))


### Bug Fixes

* `fcli aviator ssc audit`: Improve handling of `PROTOCOL_ERROR` by adding retry for failed streams ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* `fcli aviator ssc audit`: Skip suppressed issues in Aviator audit ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* `fcli aviator token *`: `--email` option is now optional in aviator token commands ([0befdb7](https://github.com/fortify/fcli/commit/0befdb7caa0d9fc6c21f35431f711ce9d8965db1))
* Action `run.fcli` instruction: Improve error handling ([5fedf4a](https://github.com/fortify/fcli/commit/5fedf4a4230c728f1a002a5a6bc6c9f05a2af609))
* Commands that output `Action` column: Fix (renamed) `__action__` property being included in output even if not explicitly listed in `-o <fmt>=<properties>` (fixes [#774](https://github.com/fortify/fcli/issues/774)) ([8352608](https://github.com/fortify/fcli/commit/835260860da7fa98f1b44649ebf34745a5266f35))
* Commands that output `Action` column: Fix `__action__` property improperly being renamed to `Action` for technical output formats like `json` or `yaml` (fixes [#774](https://github.com/fortify/fcli/issues/774)) ([8352608](https://github.com/fortify/fcli/commit/835260860da7fa98f1b44649ebf34745a5266f35))
* Commands that output `Action` column: Fix `,__action__:Action` being appended to `expr` output (fixes [#774](https://github.com/fortify/fcli/issues/774)) ([8352608](https://github.com/fortify/fcli/commit/835260860da7fa98f1b44649ebf34745a5266f35))
* SSC `setup-appversion` action: Add missing quotes to avoid exception if the name of the application version to create contains spaces ([9e0dbba](https://github.com/fortify/fcli/commit/9e0dbbac46b2dc31dbba9dfccb6d7c8c4ce0034c))
* Throw proper exception on invalid character encoding (resolves [#772](https://github.com/fortify/fcli/issues/772)) ([3fb54bb](https://github.com/fortify/fcli/commit/3fb54bb21858b9761927d58048c07f7cbecf6003))

## [3.7.0](https://github.com/fortify/fcli/compare/v3.6.0...v3.7.0) (2025-07-07)


### Features

* `fcli ssc session login`: Allow for disabling SC-SAST/SC-DAST connectivity (resolves [#740](https://github.com/fortify/fcli/issues/740)) ([b7aaae2](https://github.com/fortify/fcli/commit/b7aaae2e8676c2d900eb11a8052850b3f109b6ca))


### Bug Fixes

* `ci` action: Improve & complement usage help (fixes [#752](https://github.com/fortify/fcli/issues/752), closes [#762](https://github.com/fortify/fcli/issues/762)) ([22a5498](https://github.com/fortify/fcli/commit/22a549892a0ad61c297358d01165db6a95887be8))
* `fcli aviator ssc audit`: Fix thread synchronization issues that randomly cause exceptions while auditing ([7819ec5](https://github.com/fortify/fcli/commit/7819ec5ec7427c74fa5c6f054a4918973a42ea26))
* `gitlab-*-report` actions: Output empty string instead of `null` for `description` field ([da7f705](https://github.com/fortify/fcli/commit/da7f70526ce7350aded970c63185153807c3af2b))
* `gitlab-dast-report` FoD action: Fix exception if site tree is unavailable ([6b24369](https://github.com/fortify/fcli/commit/6b2436991e124b720ddd4b62ad58c3fb943b8e7c))
* Fix action progress messages not being cleared before final output (fixes [#766](https://github.com/fortify/fcli/issues/766)) ([4f03395](https://github.com/fortify/fcli/commit/4f033950d598575db5c7d8bd773baa5edfc4db50))
* Fix incorrect synopsis in documentation for built-in actions (fixes [#765](https://github.com/fortify/fcli/issues/765)) (closes [#767](https://github.com/fortify/fcli/issues/767)) ([4f18948](https://github.com/fortify/fcli/commit/4f189483aa44eb32d7a8392dfaf833d86a4c4158))
* SSC `check-policy` action: Fix --filterset option being ignored ([55e555d](https://github.com/fortify/fcli/commit/55e555dc96e0b22540a10c8f0c72b796e8484cac))

## [3.6.0](https://github.com/fortify/fcli/compare/v3.5.2...v3.6.0) (2025-06-14)


### Features

* `*-sast-report` actions: Add `--source-dir` option to allow for matching Fortify-reported source file paths against repository file paths (fixes [#749](https://github.com/fortify/fcli/issues/749)) ([775c5a3](https://github.com/fortify/fcli/commit/775c5a32ca65c435dd77d5eb760ddfda70796c95))
* `ci` actions: Automatically pass `--source-dir` option to SAST report actions (fixes [#749](https://github.com/fortify/fcli/issues/749)) ([775c5a3](https://github.com/fortify/fcli/commit/775c5a32ca65c435dd77d5eb760ddfda70796c95))
* `fcli fod`: New `fcli fod oss list-components` command (resolves [#244](https://github.com/fortify/fcli/issues/244)) ([775c5a3](https://github.com/fortify/fcli/commit/775c5a32ca65c435dd77d5eb760ddfda70796c95))


### Bug Fixes

* `fcli fod sast-scan setup`: Allow assessment type to be specified by Id or Name (resolves [#738](https://github.com/fortify/fcli/issues/738)) ([775c5a3](https://github.com/fortify/fcli/commit/775c5a32ca65c435dd77d5eb760ddfda70796c95))
* `fcli fod`: Fix issue with page handling in REST responses, potentially causing issues if more than 9 pages of results are available on FoD ([775c5a3](https://github.com/fortify/fcli/commit/775c5a32ca65c435dd77d5eb760ddfda70796c95))

## [3.5.2](https://github.com/fortify/fcli/compare/v3.5.1...v3.5.2) (2025-06-05)


### Bug Fixes

* `fcli aviator`: Handle 0-byte and corrupted ZIP entries during FPR processing ([3140991](https://github.com/fortify/fcli/commit/31409913b33456b4515100572ab8d029a02cd4a0))

## [3.5.1](https://github.com/fortify/fcli/compare/v3.5.0...v3.5.1) (2025-05-22)


### Bug Fixes

* `fcli aviator`: Fix `NullPointerException` when auditing certain vulnerabilities ([#744](https://github.com/fortify/fcli/issues/744)) ([4bd9e5d](https://github.com/fortify/fcli/commit/4bd9e5d8d73f068f0344f758be218acf1ff5d36f))

## [3.5.0](https://github.com/fortify/fcli/compare/v3.4.1...v3.5.0) (2025-05-19)


### Features

* `fcli fod mast-scan`: Improve `setup` and `start` commands based on FoD API improvements (fixes [#685](https://github.com/fortify/fcli/issues/685)) ([#737](https://github.com/fortify/fcli/issues/737)) ([4bdfd87](https://github.com/fortify/fcli/commit/4bdfd8793759bddeae6fc130be100c925ddba7d6))


### Bug Fixes

* `fcli aviator`: Fix reflection issues in fcli native binaries ([#736](https://github.com/fortify/fcli/issues/736)) ([acb6794](https://github.com/fortify/fcli/commit/acb6794aa69d8f73c308657d2806ea3e9b714099))

## [3.4.1](https://github.com/fortify/fcli/compare/v3.4.0...v3.4.1) (2025-04-30)


### Bug Fixes

* Fix bug in Aviator module ([7f66cbc](https://github.com/fortify/fcli/commit/7f66cbcbb0f600ad67ff3db067515290cb980fe2))

## [3.4.0](https://github.com/fortify/fcli/compare/v3.3.0...v3.4.0) (2025-04-29)


### Features

* Unhide `fcli aviator` commands for upcoming Aviator release ([0e3d0c7](https://github.com/fortify/fcli/commit/0e3d0c7a4df97ca5f5b8b400fc4f9cf3701f1386))

## [3.3.0](https://github.com/fortify/fcli/compare/v3.2.1...v3.3.0) (2025-04-25)


### Features

* Add log masking capabilities ([68a7875](https://github.com/fortify/fcli/commit/68a7875e48c8dab63eb4e09163ac5fc45842663a))


### Bug Fixes

* FoD `release-summary` action: Support FoD 24.3 (FedRAMP) ([#721](https://github.com/fortify/fcli/issues/721)) ([7c87e8d](https://github.com/fortify/fcli/commit/7c87e8dd2213620986b1aec6feb2d4bb6e079941))

## [3.2.1](https://github.com/fortify/fcli/compare/v3.2.0...v3.2.1) (2025-04-15)


### Bug Fixes

* `fcli * action run`: Apply generic fcli `--debug` option on transitive fcli invocations ([af20495](https://github.com/fortify/fcli/commit/af204952351c8c50c46e53f0fa4552dd61c571b3))
* `fcli sc action run ci`: Download server-side logs & FPR file if generic fcli `--debug` option is specified ([af20495](https://github.com/fortify/fcli/commit/af204952351c8c50c46e53f0fa4552dd61c571b3))
* `fcli sc-sast scan start`: Re-add separate option for enabling server-side diagnostics collection, independent of generic fcli `--debug` option ([af20495](https://github.com/fortify/fcli/commit/af204952351c8c50c46e53f0fa4552dd61c571b3))
* `fcli tool sc-client run`: Respect generic fcli `--debug` option to add ScanCentral Client `-debug` option ([af20495](https://github.com/fortify/fcli/commit/af204952351c8c50c46e53f0fa4552dd61c571b3))

## [3.2.0](https://github.com/fortify/fcli/compare/v3.1.1...v3.2.0) (2025-04-14)


### Features

* `ci` & `package` actions: Store ScanCentral Client log files in current working directory for easy access ([d3f604b](https://github.com/fortify/fcli/commit/d3f604b875ae3d614bd0a803aa826512dae9a33e))
* `fcli * action run package/ci`: Use generic `--debug` option to enable ScanCentral Client debug logging ([3f8b007](https://github.com/fortify/fcli/commit/3f8b007e2046c22b674c72e324025b8805e72d25))
* `fcli sc-sast scan start`: Use generic `--debug` option to enable both fcli logging and requesting ScanCentral diagnostic logs to be generated ([3f8b007](https://github.com/fortify/fcli/commit/3f8b007e2046c22b674c72e324025b8805e72d25))
* `fcli tool sc-client run`: Add `--logdir` option to specify log file location ([d3f604b](https://github.com/fortify/fcli/commit/d3f604b875ae3d614bd0a803aa826512dae9a33e))
* Add generic `--debug` flag; this enables both fcli logging, and may be used by some fcli commands or fcli actions to enable additional logging functionality ([3f8b007](https://github.com/fortify/fcli/commit/3f8b007e2046c22b674c72e324025b8805e72d25))


### Bug Fixes

* If `--log-level` was specified without `--log-file`, no log file was being generated ([3f8b007](https://github.com/fortify/fcli/commit/3f8b007e2046c22b674c72e324025b8805e72d25))

## [3.1.1](https://github.com/fortify/fcli/compare/v3.1.0...v3.1.1) (2025-04-07)


### Bug Fixes

* Fix Docker image publishing ([7426df9](https://github.com/fortify/fcli/commit/7426df9c460282fa3ce6d998222f7afcdc1776ba))

## [3.1.0](https://github.com/fortify/fcli/compare/v3.0.0...v3.1.0) (2025-04-07)


### Features

* Add FoD `servicenow-csv-report` action ([7978f8d](https://github.com/fortify/fcli/commit/7978f8d58950ef3116f46386ac0214088320fd5c))
* Add gitlab-installer-svc Docker image ([7978f8d](https://github.com/fortify/fcli/commit/7978f8d58950ef3116f46386ac0214088320fd5c))
* Add SSC `servicenow-csv-report` action ([7978f8d](https://github.com/fortify/fcli/commit/7978f8d58950ef3116f46386ac0214088320fd5c))


### Bug Fixes

* `ci-vars` action: Properly remove trailing `.git` from GitLab repo URL ([b9938b8](https://github.com/fortify/fcli/commit/b9938b80a14b8558164fc369f87ccbb6af88f11e))
* `fcli fod issue ls`: Add partial server-side filtering support ([daf4aec](https://github.com/fortify/fcli/commit/daf4aecb7426fc5abc52e7c2dffc6a66969718ae))
* Exception on YAML output if no data ([e25994d](https://github.com/fortify/fcli/commit/e25994dabc922a8a0dd6824fa3b94cf39de80b5c))
* Fix `stderr` being suppressed in `run.fcli` action step ([7e88f07](https://github.com/fortify/fcli/commit/7e88f0731f1ecdef583d542cc9dcb79ab26be750))
* Fix table output exception (fixes [#708](https://github.com/fortify/fcli/issues/708)) ([24e70e3](https://github.com/fortify/fcli/commit/24e70e3090ddc037d883b6a319aacf8beb9a7f7d))
* Improve output on REST response exceptions ([e051bcc](https://github.com/fortify/fcli/commit/e051bcc30c1f2bd8e5746b726ee0251076726133))

## [3.0.0](https://github.com/fortify/fcli/compare/v2.12.2...v3.0.0) (2025-03-18)


### ⚠ BREAKING CHANGES

* `fcli`:`--output` option: Removed some output formats, partially replaced by new `--style` option
* `fcli fod`: Renamed `--session` option to `--fod-session`
* `fcli * action`: Significant changes to fcli action yaml syntax; custom actions developed for fcli 2.x will not run on fcli 3.x, and vice versa
* `fcli sc-dast session`: All SC-DAST session commands have been removed; please use `fcli ssc session` commands instead
* `fcli sc-dast`: Renamed `--session` option to `--ssc-session`
* `fcli sc-sast session`: All SC-SAST session commands have been removed; please use `fcli ssc session` commands instead
* `fcli sc-sast`: Renamed `--session` option to `--ssc-session`
* `fcli sc-sast scan start`: Local files referenced in `--sargs` must now be preceded with `@`, not `file:`. This is a shorter, more common convention for referencing files.
* `fcli sc-sast scan start`: Renamed `--ssc-ci-token` option to `--publish-token` to better describe the purpose
* `fcli sc-sast scan start`: Remove `-p / --package-file` option; replaced by `-f / --file`
* `fcli sc-sast scan start`: Remove `-m / --mbs-file` option; replaced by `-f / --file`
* `fcli ssc session`: Now manages combined SSC/SC-SAST/SC-DAST sessions, allowing a single session to be used for invoking all SSC/SC-SAST/SC-DAST commands
* `fcli ssc`: Renamed `--session` option to `--ssc-session`
* `fcli ssc session login`: Removed `--ci-token` option; please use `--token` option instead
* `fcli ssc appversion create`: Removed deprecated `AnalysisProcessingRules` as allowed value for `--copy` option; use `processing-rules` instead
* `fcli ssc appversion create`: Removed deprecated `BugTrackerConfiguration` as allowed value for `--copy` option; use `bugtracker` instead
* `fcli ssc issue`: Removed hidden/preview `check` command, as this is now implemented through the `check-policy` action

### Features

* `fcli * action`: New `package` action for packaging source code using ScanCentral Client ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli * action`: Significant changes to fcli action yaml syntax; custom actions developed for fcli 2.x will not run on fcli 3.x, and vice versa ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli action`: New top-level action command for cross-product or product-agnostic actions ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli aviator`: New module to manage Fortify Aviator and run Aviator audits (hidden until Aviator has been released) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli config`: Add ability to configure fcli trust store through environment variables ([#690](https://github.com/fortify/fcli/issues/690)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod app create`: New `--skip-if-exists` option ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod issue`: New `update` command (resolves fortify[#669](https://github.com/fortify/fcli/issues/669)) ([#698](https://github.com/fortify/fcli/issues/698)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod`: Renamed `--session` option to `--fod-session` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-dast session`: All SC-DAST session commands have been removed; please use `fcli ssc session` commands instead ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-dast`: Renamed `--session` option to `--ssc-session` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan download`: New command for downloading FPR, logs, job files ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan list`: New command for listing scan jobs ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Add `--debug` option to request debug (diagnosis) logs to be collected for the scan job ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Add `--no-replace` option to keep existing scan jobs ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Add `--publish-as` option to specify the name of the FPR file that is uploaded to SSC ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Add `--scan-timeout` option to specify scan job time-out ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Add `-f / --file` option to specify scan payload; automatically detects MBS or package file ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Local files referenced in `--sargs` must now be preceded with `@`, not `file:`. This is a shorter, more common convention for referencing files. ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Remove `-m / --mbs-file` option; replaced by `-f / --file` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Remove `-p / --package-file` option; replaced by `-f / --file` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Renamed `--ssc-ci-token` option to `--publish-token` to better describe the purpose ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast session`: All SC-SAST session commands have been removed; please use `fcli ssc session` commands instead ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast`: Renamed `--session` option to `--ssc-session` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc action`: Add support for `sc-sast` and `sc-dast` request targets in action implementations ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc appversion create`: Removed deprecated `AnalysisProcessingRules` as allowed value for `--copy` option; use `processing-rules` instead ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc appversion create`: Removed deprecated `BugTrackerConfiguration` as allowed value for `--copy` option; use `bugtracker` instead ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc issue`: Removed hidden/preview `check` command, as this is now implemented through the `check-policy` action ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc session login`: Default session lifetime when authenticating with user credentials is now 3 days for recent SSC versions, instead of only 1 day ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc session login`: New `--client-auth-token` option due to SC-SAST sessions now being managed through SSC sessions ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc session login`: New `--sc-sast-url` option due to SC-SAST sessions now being managed through SSC sessions ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc session login`: Removed `--ci-token` option; please use `--token` option instead ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc session`: Now manages combined SSC/SC-SAST/SC-DAST sessions, allowing a single session to be used for invoking all SSC/SC-SAST/SC-DAST commands ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc`: Renamed `--session` option to `--ssc-session` ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli tool`: Allow cached tool installations to be re-used if fcli state information is lost (for example across different CI pipeline runs) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli tool`: New `run` commands for directly running installed tools through fcli ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli`: New `--style` option to allow for overriding default output styles ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli`:`--output` option: Removed some output formats, partially replaced by new `--style` option ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))


### Bug Fixes

* `fcli fod action`: `gitlab-sast-report`: Output empty string instead of `null` for description field ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod action`: `setup-release`: Add tech stack and language level options (fixes [#691](https://github.com/fortify/fcli/issues/691)) ([#692](https://github.com/fortify/fcli/issues/692)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod app create`: Allow for optional or numeric owner (fixes [#686](https://github.com/fortify/fcli/issues/686)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod dast-scan start-legacy`: New `--validate-entitlement` option to validate entitlement is defined and/or valid (fixes [#682](https://github.com/fortify/fcli/issues/682)) ([#684](https://github.com/fortify/fcli/issues/684)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod dast-scan start`: New `--validate-entitlement` option to validate entitlement is defined and/or valid (fixes [#682](https://github.com/fortify/fcli/issues/682)) ([#684](https://github.com/fortify/fcli/issues/684)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod mast-scan start`: New `--validate-entitlement` option to validate entitlement is defined and/or valid (fixes [#682](https://github.com/fortify/fcli/issues/682)) ([#684](https://github.com/fortify/fcli/issues/684)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod oss-scan start`: New `--validate-entitlement` option to validate entitlement is defined and/or valid (fixes [#682](https://github.com/fortify/fcli/issues/682)) ([#684](https://github.com/fortify/fcli/issues/684)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli fod sast-scan start`: New `--validate-entitlement` option to validate entitlement is defined and/or valid (fixes [#682](https://github.com/fortify/fcli/issues/682)) ([#684](https://github.com/fortify/fcli/issues/684)) ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan start`: Request Linux sensor if package contains file names that are incompatible with Windows sensors ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli sc-sast scan status`: Use v4 endpoint to retrieve SSC-related properties ([2a9e69e](https://github.com/fortify/fcli/commit/2a9e69ef9c5b6e85914caee1b9c2093f575dc0bb))
* `fcli ssc report`: Add missing report types (fixes [#697](https://github.com/fortify/fcli/issues/697)) ([bd5187b](https://github.com/fortify/fcli/commit/bd5187bf48b4b237d134eb8ae4460190efc6f719))

## [2.12.3](https://github.com/fortify/fcli/compare/v2.12.2...v2.12.3) (2025-03-12)


### Bug Fixes

* Refreshed build with updated tool definitions ([870e3cd](https://github.com/fortify/fcli/commit/870e3cd914a75ceb957c8573df979c00950533d4))

## [2.12.2](https://github.com/fortify/fcli/compare/v2.12.1...v2.12.2) (2025-01-21)


### Bug Fixes

* `fcli fod action run github-sast-report`: Add severity data to report ([1e80d5e](https://github.com/fortify/fcli/commit/1e80d5efb483088aaa52b565848a089649aa7133))
* `fcli fod action run sarif-sast-report`: Add severity data to report ([1e80d5e](https://github.com/fortify/fcli/commit/1e80d5efb483088aaa52b565848a089649aa7133))
* `fcli ssc action run github-sast-report`: Add severity data to report ([1e80d5e](https://github.com/fortify/fcli/commit/1e80d5efb483088aaa52b565848a089649aa7133))
* `fcli ssc action run sarif-sast-report`: Add severity data to report ([1e80d5e](https://github.com/fortify/fcli/commit/1e80d5efb483088aaa52b565848a089649aa7133))

## [2.12.1](https://github.com/fortify/fcli/compare/v2.12.0...v2.12.1) (2025-01-07)


### Bug Fixes

* `fcli ssc av create`: `--copy-from` option now copies all attribute values (fixes [#666](https://github.com/fortify/fcli/issues/666)) ([5a32f3f](https://github.com/fortify/fcli/commit/5a32f3f842be9164a590ef0c9754fd3ce059ec8c))

## [2.12.0](https://github.com/fortify/fcli/compare/v2.11.1...v2.12.0) (2024-12-23)


### Features

* `fcli fod dast setup-website`, `fcli fod dast setup-workflow`, `fcli fod dast setup-api`: Add `--vpn` option for specifying Fortify Connect network name (site-to-site VPN) to use (fixes [#644](https://github.com/fortify/fcli/issues/644)) ([8e38b94](https://github.com/fortify/fcli/commit/8e38b9422c880d5aee0191c2289ed235b6483b06))
* `fcli fod mast setup`, `fcli fod mast get-config`: Updates for new API (fixes [#642](https://github.com/fortify/fcli/issues/642)) ([8e38b94](https://github.com/fortify/fcli/commit/8e38b9422c880d5aee0191c2289ed235b6483b06))
* `fcli tool sc-client install`: Add options to install compatible JRE ([85bc662](https://github.com/fortify/fcli/commit/85bc662c3a6c1214fd37f92165b40390f01c83ec))


### Bug Fixes

* `fcli fod action run release-summary`: Improve/simply based on FoD 24.4 API changes ([8e38b94](https://github.com/fortify/fcli/commit/8e38b9422c880d5aee0191c2289ed235b6483b06))
* `fcli fod release update`: Add "Retired" option fo `--sdlc-status` (fixes [#642](https://github.com/fortify/fcli/issues/642)) ([8e38b94](https://github.com/fortify/fcli/commit/8e38b9422c880d5aee0191c2289ed235b6483b06))
* fcli fod action run release-summary update (fixes [#639](https://github.com/fortify/fcli/issues/639)) ([b7e16c4](https://github.com/fortify/fcli/commit/b7e16c495bd50961b3aee1fc34272a981563dd24))

## [2.11.1](https://github.com/fortify/fcli/compare/v2.11.0...v2.11.1) (2024-12-11)


### Bug Fixes

* `fcli fod action run github-pr-comment`: Use `GITHUB_API_URL` environment variable instead of hardcoded api.github.com to avoid failure on GitHub Enterprise ([da7eba3](https://github.com/fortify/fcli/commit/da7eba35795e7ddd78084d139e0c422f2a86707a))
* `fcli ssc action run github-pr-comment`: Use `GITHUB_API_URL` environment variable instead of hardcoded api.github.com to avoid failure on GitHub Enterprise ([da7eba3](https://github.com/fortify/fcli/commit/da7eba35795e7ddd78084d139e0c422f2a86707a))

## [2.11.0](https://github.com/fortify/fcli/compare/v2.10.1...v2.11.0) (2024-12-11)


### Features

* `fcli ssc appversion list`: Add `--exclude` option to allow for excluding empty versions, or versions that have no issues assigned to current user ([ba0c126](https://github.com/fortify/fcli/commit/ba0c1263e3bafc730037c747c9bcce964dd5cfe6))
* `fcli ssc appversion list`: Add `--include` option to allow for listing active, inactive, or both active and inactive versions ([ba0c126](https://github.com/fortify/fcli/commit/ba0c1263e3bafc730037c747c9bcce964dd5cfe6))

## [2.10.1](https://github.com/fortify/fcli/compare/v2.10.0...v2.10.1) (2024-12-05)


### Bug Fixes

* `fcli sc-sast scan start`: Output root exception if error occurs while determining .NET version ([0bb7260](https://github.com/fortify/fcli/commit/0bb7260eb664c0b90c68e7a422a09f6e0f14bc7b))

## [2.10.0](https://github.com/fortify/fcli/compare/v2.9.1...v2.10.0) (2024-11-21)


### Features

* `fcli sc-sast session login`: Allow for overriding SC SAST Controller URL (resolves [#611](https://github.com/fortify/fcli/issues/611)) ([a5eb382](https://github.com/fortify/fcli/commit/a5eb3826202f3399159541bb2a68c295654bc9ea))
* `fcli ssc appversion update`: Add `--active` option to allow activating/deactivating applications versions (resolves [#625](https://github.com/fortify/fcli/issues/625)) ([#647](https://github.com/fortify/fcli/issues/647)) ([c2c9a33](https://github.com/fortify/fcli/commit/c2c9a33ae001df56c9e502d622129326392ec14d))


### Bug Fixes

* `fcli ssc artifact get`: Include scan data in output (resolves [#637](https://github.com/fortify/fcli/issues/637)) ([e6f1a3e](https://github.com/fortify/fcli/commit/e6f1a3eb731d8e923abc444cd2553613c6fd4714))

## [2.9.1](https://github.com/fortify/fcli/compare/v2.9.0...v2.9.1) (2024-11-07)


### Bug Fixes

* `fcli ssc action run appversion-summary`: Add note about removed issue count ([0c93649](https://github.com/fortify/fcli/commit/0c936492a4850d1d322d4ab3b49497b2e91866a2))
* `fcli ssc action run appversion-summary`: Fix exception if application version has artifacts with 0 issues (fixes [#633](https://github.com/fortify/fcli/issues/633)) ([c89817d](https://github.com/fortify/fcli/commit/c89817deedb3eb11db93dd9757e9106cb516655c))

## [2.9.0](https://github.com/fortify/fcli/compare/v2.8.0...v2.9.0) (2024-10-30)


### Features

* `fcli fod action run setup-release`: Add support for creating parent application & microservice if not existing ([9e3a8fd](https://github.com/fortify/fcli/commit/9e3a8fd4d1e49195eee9468ab01d52c93c0688f0))
* `fcli fod release create`: Add support for creating parent application & microservice if not existing ([576b620](https://github.com/fortify/fcli/commit/576b6200f28ff96f159e9f154562895b7c245259))
* `fcli fod release create`: Ignore `--copy-from` if equal to release being created ([576b620](https://github.com/fortify/fcli/commit/576b6200f28ff96f159e9f154562895b7c245259))
* `fcli fod release create`: Ignore `--copy-from` if first release on new application ([576b620](https://github.com/fortify/fcli/commit/576b6200f28ff96f159e9f154562895b7c245259))
* `fcli fod release create`: Throw user-friendly error when trying to copy release from different application ([576b620](https://github.com/fortify/fcli/commit/576b6200f28ff96f159e9f154562895b7c245259))


### Bug Fixes

* Improve parsing of boolean action parameters ([d3b6f4c](https://github.com/fortify/fcli/commit/d3b6f4c260580c6db7f0edcc175dc354db72eae1))

## [2.8.0](https://github.com/fortify/fcli/compare/v2.7.1...v2.8.0) (2024-10-25)


### Features

* `fcli sc-sast scan start`: Add support for passing scan arguments through `--sargs` option (resolves [#449](https://github.com/fortify/fcli/issues/449)) ([#627](https://github.com/fortify/fcli/issues/627)) ([7920a40](https://github.com/fortify/fcli/commit/7920a40b0d395dc787df7d7ea621402c29545b7c))
* Add `fcli fod release wait-for` command to wait for release(s) to leave suspended state (resolves [#624](https://github.com/fortify/fcli/issues/624)) ([0cdde30](https://github.com/fortify/fcli/commit/0cdde30583e5ba270cf3f605fedc64511164f8b8))


### Bug Fixes

* `fcli fod action run setup-release`: Add `Development` default value for `--sdlc-status` ([9a1b1bf](https://github.com/fortify/fcli/commit/9a1b1bff3fe2b68fcc35f9512cf1a85f3713691c))
* `fcli fod action run setup-release`: Wait for release to exit suspended state ([07d0914](https://github.com/fortify/fcli/commit/07d0914964700adf33712f65b92817778b2178f2))
* Fix fcli command links in action documentation (fixes [#622](https://github.com/fortify/fcli/issues/622)) ([fecf423](https://github.com/fortify/fcli/commit/fecf423d6d4a6cb5a31445a4bdb75b5906658be5))

## [2.7.1](https://github.com/fortify/fcli/compare/v2.7.0...v2.7.1) (2024-09-27)


### Bug Fixes

* Fix fcli completion script sourcing error (fixes [#580](https://github.com/fortify/fcli/issues/580)) ([4ff86f4](https://github.com/fortify/fcli/commit/4ff86f46a25045cae6bec89cdb25815905db2ed6))
* FoD `release-summary` action: Fix potential SpEL exception for releases with open-source scans enabled (fixes [#612](https://github.com/fortify/fcli/issues/612)) ([5260bc8](https://github.com/fortify/fcli/commit/5260bc89fd770a14fe435bd6a89f719011087f94))
* Improve synopsis order (fixes [#133](https://github.com/fortify/fcli/issues/133)) ([78b530c](https://github.com/fortify/fcli/commit/78b530cde00e514be59377d2f80d6479a90d893b))
* Show proper syntax for `--store` option in help output (fixes [#613](https://github.com/fortify/fcli/issues/613)) ([cac574d](https://github.com/fortify/fcli/commit/cac574da483908a393f747a82838070badc7675d))

## [2.7.0](https://github.com/fortify/fcli/compare/v2.6.0...v2.7.0) (2024-09-25)


### Features

* `fcli fod release create`:  Support release attributes (fixes fortify[#592](https://github.com/fortify/fcli/issues/592)) ([3727329](https://github.com/fortify/fcli/commit/37273298d007a15c552ae308b77f59d5e744798a))
* `fcli fod sast-scan setup`: Add `--skip-if-exists` option ([edcece5](https://github.com/fortify/fcli/commit/edcece5bf8b13aa5e79fd39ffa23ab7e03132781))
* `fcli fod sast-scan setup`: Add `--use-aviator` option (fixes fortify[#594](https://github.com/fortify/fcli/issues/594)) ([013af6f](https://github.com/fortify/fcli/commit/013af6ff7d4a0f1a140764447a34556960b51df7))
* `fcli fod sast-scan setup`: Set `--technology-stack` to `Auto Detect` by default (fixes [#595](https://github.com/fortify/fcli/issues/595)) ([852d7bf](https://github.com/fortify/fcli/commit/852d7bfa36af8a34c7eec768e2ddc6e81e33b2b1))
* `fcli sc-sast scan start`: Add option to select sensor pool for the scan ([d071d25](https://github.com/fortify/fcli/commit/d071d25944fbd06b79bc622d323fa6d42b5d75ba))
* `fcli ssc appversion copy-state`: Add `--refresh-timeout` option ([89cf435](https://github.com/fortify/fcli/commit/89cf4351ec3eb8fe9dfbdd682b8d485ee9bff07b))
* `fcli ssc appversion create`: Add `--refresh-timeout` option ([89cf435](https://github.com/fortify/fcli/commit/89cf4351ec3eb8fe9dfbdd682b8d485ee9bff07b))
* Add `fcli sc-sast sensor-pool list` command ([77fcc1c](https://github.com/fortify/fcli/commit/77fcc1c57f044aa99ba49e77710b98fd062593c0))
* Add FoD setup-release action ([4ab86c0](https://github.com/fortify/fcli/commit/4ab86c066a730fa10c29d4ab18f4838e322c1327))
* Add SSC setup-appversion action ([e3a273c](https://github.com/fortify/fcli/commit/e3a273c5ce489311f19f2355fde3f383bfac43a2))
* FoD & SSC: Add aws-sast-report actions to enable integrating Fortify results with AWS Security Hub ([#559](https://github.com/fortify/fcli/issues/559)) ([dc79095](https://github.com/fortify/fcli/commit/dc790950794c976c5242fc44fffd2ad5c0f1c081))


### Bug Fixes

* `fcli fod app update`: Ignore release attributes if included in `--attrs` option (fixes fortify[#604](https://github.com/fortify/fcli/issues/604)) ([e2077b9](https://github.com/fortify/fcli/commit/e2077b942e413083707893cad9b2aff562b391c6))
* `fcli fod release create`: Ignore application attributes if included in `--attrs` option (fixes fortify[#604](https://github.com/fortify/fcli/issues/604)) ([e2077b9](https://github.com/fortify/fcli/commit/e2077b942e413083707893cad9b2aff562b391c6))
* `fcli fod release update`: Ignore application attributes if included in `--attrs` option (fixes fortify[#604](https://github.com/fortify/fcli/issues/604)) ([e2077b9](https://github.com/fortify/fcli/commit/e2077b942e413083707893cad9b2aff562b391c6))
* `fcli ssc appversion refresh-metrics`: Allow for `fcli state wait-for-job ::var::` to be invoked without errors even if no refresh was required ([89cf435](https://github.com/fortify/fcli/commit/89cf4351ec3eb8fe9dfbdd682b8d485ee9bff07b))
* Increase issue limit for `github-sast-report` to match current GitHub limits ([3a2d489](https://github.com/fortify/fcli/commit/3a2d48929a9978ab71f0e02d2d669b47686556ce))

## [2.6.0](https://github.com/fortify/fcli/compare/v2.5.3...v2.6.0) (2024-09-09)


### Features

* Publish fortifydocker/fcli image ([c72487d](https://github.com/fortify/fcli/commit/c72487d834c966cf468088290b4e41bae5b5156a))


### Bug Fixes

* `fcli fod action run *-sast-report`: Warn instead of fail if scan summary is not (yet) available from FoD ([077157f](https://github.com/fortify/fcli/commit/077157f15f4885ebafe1ef7e7b50b276147b4e1d))
* FoD: Improve help output for `fcli fod *-scan wait-for` commands ([#587](https://github.com/fortify/fcli/issues/587)) ([937baf5](https://github.com/fortify/fcli/commit/937baf58c00734c3018c76800c585dbeaadf0cd7))
* Work-around for user.home in Docker images ([9c6a56c](https://github.com/fortify/fcli/commit/9c6a56c1ca9e6781ecd029e53fb0b7a4bb8de45e))

## [2.5.3](https://github.com/fortify/fcli/compare/v2.5.2...v2.5.3) (2024-08-30)


### Bug Fixes

* Fix error on `fcli ssc session login` command on older SSC versions (fixes [#584](https://github.com/fortify/fcli/issues/584)) ([d028052](https://github.com/fortify/fcli/commit/d02805231abf4bc7afeab3c719db56ef46b50c2c))

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
