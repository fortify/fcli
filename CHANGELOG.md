# Changelog

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
