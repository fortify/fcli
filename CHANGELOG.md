# Changelog

## [1.2.4](https://github.com/fortify/fcli/compare/v1.2.2...v1.2.4) (2023-04-07)


### Bug Fixes

* `fcli ssc appversion-artifact download`: Include externalmetadata.xml in current state FPR download by passing arbitrary clientVersion parameter to SSC (fixes [#257](https://github.com/fortify/fcli/issues/257)) ([2694ffe](https://github.com/fortify/fcli/commit/2694ffe0224d85121ea0eaadda64464a0f6f3ff5))
* `fcli tool vuln-exporter install`: Add support for latest (2.0.1) version ([9c34f73](https://github.com/fortify/fcli/commit/9c34f73eb4b7b5474e742d138b908cff6042f438))


### Miscellaneous Chores

* release 1.2.4 ([4f23048](https://github.com/fortify/fcli/commit/4f230489e566f6e647f45b040053026909b6cb58))

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
