# Changelog

## 1.2.4 (2023-08-18)


### ⚠ BREAKING CHANGES

* Restructure SSC commands
* Refactoring & improve `fcli util all-commands`
* `fcli sc-dast scan start`: Change short option names to lowercase for consistency (fixes #325)
* `fcli sc-dast scan start`: Remove --start-urls option; not supported on SC-DAST (fixes #324)
* Change `--no-progress` to `--progress <type>` (closes #305)
* `fcli ssc report-template generate-answerfile`: Renamed to `generate-config` for consistency, changed option names
* `fcli ssc report-template create --anser-file`: Rename to `--config`
* `fcli sc-dast scan list`: Replace individual server-side query options with generic `--server-query` option for consistency with other commands and SSC/FoD modules
* `fcli sc-dast scan-settings list`: Replace individual server-side query options with generic `--server-query` option for consistency with other commands and SSC/FoD modules
* FoD: changes to a number of options to standardize arity (fixes #268)
* `fcli ssc appversion * --attribute`: Rename to `--attributes` for consistency; repeatable option accepting comma-separated list of attributes (resolves #269)
* `fcli ssc appversion * --useradd`: Rename to `--add-users` for consistency; repeatable option accepting comma-separated list of users (resolves #269)
* `fcli ssc appversion * --userdel`: Rename to `--rm-users` for consistency; repeatable option accepting comma-separated list of users (resolves #269)
* `fcli sc-dast scan start --start-url`: Rename to `--start-urls` for consistency; repeatable option accepting comma-separated list of URLs (resolves #269)
* `fcli ssc role create --permission-id`: Rename to `--permission-ids` for consistency; repeatable option accepting comma-separated list of permission id's (resolves #269)
* `fcli ssc appversion * --attribute`: Rename to `--attributes` for consistency; repeatable option accepting comma-separated list of attributes (resolves #269)
* `fcli ssc appversion * --useradd`: Rename to `--add-users` for consistency; repeatable option accepting comma-separated list of users (resolves #269)
* `fcli ssc appversion * --userdel`: Rename to `--rm-users` for consistency; repeatable option accepting comma-separated list of users (resolves #269)
* `fcli sc-dast scan start --start-url`: Rename to `--start-urls` for consistency; repeatable option accepting comma-separated list of URLs (resolves #269)
* `fcli ssc role create --permission-id`: Rename to `--permission-ids` for consistency; repeatable option accepting comma-separated list of permission id's (resolves #269)
* FoD: changes to a number of options to standardize arity (fixes #268)
* FoD: refactor `fcli fod app` creation commands (implements #266)
* `-q` option now takes an [SpEL](https://docs.spring.io/spring-framework/docs/6.0.x/reference/html/core.html#expressions) expression; existing fcli invocations may need to be updated to use the new query format (resolves #265, resolves #172)
* `-o expr=...` now evaluates expressions between curly braces using [SpEL](https://docs.spring.io/spring-framework/docs/6.0.x/reference/html/core.html#expressions) instead of JSONPath. Existing expressions with simple property references are not affected by this change, but more advanced JSONPath expressions will need to be rewritten to use SpEL instead.
* The .jar version of fcli now requires Java 17 to run (previously Java 11 was required)
* `fcli ssc report-template generate-answerFile`: Rename command to `generate-answerfile` for consistency
* `fcli ssc report-template generate-answerFile`: Rename `--force` to `--confirm` for consistency
* `fcli ssc app delete`: Rename `--delete-versions` to `--confirm`
* `fcli tool * install`: Rename `--replace-existing` to `--confirm`
* `fcli tool * uninstall`: Rename `--confirm-uninstall` to `--confirm`
* Lookup and query values now use case-sensitive matching, to avoid inconsistent behavior with case-sensitive server-side matching and case-insensitive client-side matching (fixes #125, fixes #185)
* `fcli sc-dast scan retry import-results`: Rename to `fcli sc-dast scan publish` as this can also be used for initial publishing
* `fcli sc-dast scan retry import-findings`: Rename to `fcli sc-dast scan import-findings` to reduce command tree depth
* `fcli config ssl truststore`: Rename command tree to `fcli config truststore`
* `fcli state var`: Restructure variable-related commands
* `fcli ssc appversion-artifact download`: Move application version state download to separate `fcli ssc appversion-artifact download-state` command
* `fcli ssc appversion-artifact download`: Add alias `download-by-id` to differentatie from `download-state`
* `fcli ssc appversion-artifact download`: Change artifact id option to positional parameter
* `fcli ssc appversion-artifact import debricked`: Rename command to `fcli ssc appversion-artifact import-debricked`
* `fcli ssc appversion-artifact purge by-id`: Rename command to `fcli ssc appversion-artifact purge-by-id`
* `fcli ssc appversion-artifact purge by-date`: Rename command to `fcli ssc appversion-artifact purge-older-than`
* `fcli ssc appversion-artifact purge by-date`: Change `--older-than` option to positional parameter
* `fcli config`: Change location of configuration files; you may need to manually clean up old configuration files and re-apply configuration settings like proxy and trust store (closes #238)
* `fcli config var`: Move location of variable data; you may need to manually clean up old variable state files and recreate any persisted variables (closes #239)
* `fcli * session`: Move location of session data; you may need to manually clean up old session state files and run `fcli * session login` again (closes #239)
* Cleanup: Easiest approach to clean up old configuration and state data is to delete the fcli data directory (usually <user-home>/.fortify/fcli) before you start using this new fcli version
* `fcli config var`: Move variable-related commands to `fcli state var` (closes #237)
* Environment: Rename `FORTIFY_HOME` and `FCLI_HOME` environment variables to `FORTIFY_DATA_DIR` and `FCLI_DATA_DIR` (closes #248)
* Remove support for predefined `?` variables (resolves #160)
* Change syntax for referencing variables from `{?var:prop}` to `::var::prop` (resolves #160)

### Features

* `-o expr=...` now evaluates expressions between curly braces using [SpEL](https://docs.spring.io/spring-framework/docs/6.0.x/reference/html/core.html#expressions) instead of JSONPath. Existing expressions with simple property references are not affected by this change, but more advanced JSONPath expressions will need to be rewritten to use SpEL instead. ([7f4a743](https://github.com/fortify/fcli/commit/7f4a7431c3101770b23f92dc9483b842d6302b86))
* `-o json-properties` now also outputs the type of each property for informational purposes ([7f4a743](https://github.com/fortify/fcli/commit/7f4a7431c3101770b23f92dc9483b842d6302b86))
* `-q` option now takes an [SpEL](https://docs.spring.io/spring-framework/docs/6.0.x/reference/html/core.html#expressions) expression; existing fcli invocations may need to be updated to use the new query format (resolves [#265](https://github.com/fortify/fcli/issues/265), resolves [#172](https://github.com/fortify/fcli/issues/172)) ([7f4a743](https://github.com/fortify/fcli/commit/7f4a7431c3101770b23f92dc9483b842d6302b86))
* `fcli * rest call`: Add `--no-paging` and `--transform` options ([af5867c](https://github.com/fortify/fcli/commit/af5867cf3bbe0a251aa7ad0fb118a844e0bb4e0f))
* `fcli * rest call`: Add support for taking file input for request body (closes [#342](https://github.com/fortify/fcli/issues/342)) ([f6e860d](https://github.com/fortify/fcli/commit/f6e860d193367adc39390bf08993746b56f6a4ed))
* `fcli * rest call`: Apply generic transformations by default, add --no-transform option to disable tranformations ([a0b022b](https://github.com/fortify/fcli/commit/a0b022b73b689a848fc6a0ce6573d905948f598a))
* `fcli * session`: Move location of session data; you may need to manually clean up old session state files and run `fcli * session login` again (closes [#239](https://github.com/fortify/fcli/issues/239)) ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli config clear`: Add support for interactive confirmation ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli config clear`: Clear only configuration data, not state data ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli config ssl truststore`: Rename command tree to `fcli config truststore` ([e8bedf1](https://github.com/fortify/fcli/commit/e8bedf16a634a57f77dd38306358c4573601fd8d))
* `fcli config var`: Move location of variable data; you may need to manually clean up old variable state files and recreate any persisted variables (closes [#239](https://github.com/fortify/fcli/issues/239)) ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli config var`: Move variable-related commands to `fcli state var` (closes [#237](https://github.com/fortify/fcli/issues/237)) ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli config`: Change location of configuration files; you may need to manually clean up old configuration files and re-apply configuration settings like proxy and trust store (closes [#238](https://github.com/fortify/fcli/issues/238)) ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli sc-dast scan list`: Replace individual server-side query options with generic `--server-query` option for consistency with other commands and SSC/FoD modules ([11ef076](https://github.com/fortify/fcli/commit/11ef076493012d492a07d565d63524ea41de4a01))
* `fcli sc-dast scan retry import-findings`: Rename to `fcli sc-dast scan import-findings` to reduce command tree depth ([098a5f3](https://github.com/fortify/fcli/commit/098a5f3191270c84d5c062cfa5a57fe4154fbffa))
* `fcli sc-dast scan retry import-results`: Rename to `fcli sc-dast scan publish` as this can also be used for initial publishing ([098a5f3](https://github.com/fortify/fcli/commit/098a5f3191270c84d5c062cfa5a57fe4154fbffa))
* `fcli sc-dast scan start --start-url`: Rename to `--start-urls` for consistency; repeatable option accepting comma-separated list of URLs (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([caa8061](https://github.com/fortify/fcli/commit/caa8061588aa2861410ca8ba1efba3f3d75f9949))
* `fcli sc-dast scan start --start-url`: Rename to `--start-urls` for consistency; repeatable option accepting comma-separated list of URLs (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([f4a0a6e](https://github.com/fortify/fcli/commit/f4a0a6ec6df9e8e41a3114fc0e3ebab6a6390f95))
* `fcli sc-dast scan-settings list`: Replace individual server-side query options with generic `--server-query` option for consistency with other commands and SSC/FoD modules ([11ef076](https://github.com/fortify/fcli/commit/11ef076493012d492a07d565d63524ea41de4a01))
* `fcli ssc app delete`: Add support for interactive confirmation ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli ssc app delete`: Rename `--delete-versions` to `--confirm` ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli ssc appversion * --attribute`: Rename to `--attributes` for consistency; repeatable option accepting comma-separated list of attributes (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([caa8061](https://github.com/fortify/fcli/commit/caa8061588aa2861410ca8ba1efba3f3d75f9949))
* `fcli ssc appversion * --attribute`: Rename to `--attributes` for consistency; repeatable option accepting comma-separated list of attributes (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([f4a0a6e](https://github.com/fortify/fcli/commit/f4a0a6ec6df9e8e41a3114fc0e3ebab6a6390f95))
* `fcli ssc appversion * --useradd`: Rename to `--add-users` for consistency; repeatable option accepting comma-separated list of users (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([caa8061](https://github.com/fortify/fcli/commit/caa8061588aa2861410ca8ba1efba3f3d75f9949))
* `fcli ssc appversion * --useradd`: Rename to `--add-users` for consistency; repeatable option accepting comma-separated list of users (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([f4a0a6e](https://github.com/fortify/fcli/commit/f4a0a6ec6df9e8e41a3114fc0e3ebab6a6390f95))
* `fcli ssc appversion * --userdel`: Rename to `--rm-users` for consistency; repeatable option accepting comma-separated list of users (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([caa8061](https://github.com/fortify/fcli/commit/caa8061588aa2861410ca8ba1efba3f3d75f9949))
* `fcli ssc appversion * --userdel`: Rename to `--rm-users` for consistency; repeatable option accepting comma-separated list of users (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([f4a0a6e](https://github.com/fortify/fcli/commit/f4a0a6ec6df9e8e41a3114fc0e3ebab6a6390f95))
* `fcli ssc appversion get|list`: Add embed functionality ([faebf8a](https://github.com/fortify/fcli/commit/faebf8a5911a75d7157fc1fe0bbfda7f48297228))
* `fcli ssc appversion-artifact download`: Add alias `download-by-id` to differentatie from `download-state` ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact download`: Change artifact id option to positional parameter ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact download`: Move application version state download to separate `fcli ssc appversion-artifact download-state` command ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact import debricked`: Rename command to `fcli ssc appversion-artifact import-debricked` ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact purge by-date`: Change `--older-than` option to positional parameter ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact purge by-date`: Rename command to `fcli ssc appversion-artifact purge-older-than` ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc appversion-artifact purge by-id`: Rename command to `fcli ssc appversion-artifact purge-by-id` ([3d96cfd](https://github.com/fortify/fcli/commit/3d96cfddf976486a77e65fa26f9b883b71f89ab3))
* `fcli ssc report-template create --anser-file`: Rename to `--config` ([a85b226](https://github.com/fortify/fcli/commit/a85b22600d89342cf8e8b68ba3af3fd210a063a1))
* `fcli ssc report-template generate-answerFile`: Add support for interactive confirmation ([ec6df34](https://github.com/fortify/fcli/commit/ec6df34f8b0157a01f15d1d10e0f237d249e71dc))
* `fcli ssc report-template generate-answerFile`: Rename `--force` to `--confirm` for consistency ([ec6df34](https://github.com/fortify/fcli/commit/ec6df34f8b0157a01f15d1d10e0f237d249e71dc))
* `fcli ssc report-template generate-answerFile`: Rename command to `generate-answerfile` for consistency ([ec6df34](https://github.com/fortify/fcli/commit/ec6df34f8b0157a01f15d1d10e0f237d249e71dc))
* `fcli ssc report-template generate-answerfile`: Renamed to `generate-config` for consistency, changed option names ([a85b226](https://github.com/fortify/fcli/commit/a85b22600d89342cf8e8b68ba3af3fd210a063a1))
* `fcli ssc role create --permission-id`: Rename to `--permission-ids` for consistency; repeatable option accepting comma-separated list of permission id's (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([caa8061](https://github.com/fortify/fcli/commit/caa8061588aa2861410ca8ba1efba3f3d75f9949))
* `fcli ssc role create --permission-id`: Rename to `--permission-ids` for consistency; repeatable option accepting comma-separated list of permission id's (resolves [#269](https://github.com/fortify/fcli/issues/269)) ([f4a0a6e](https://github.com/fortify/fcli/commit/f4a0a6ec6df9e8e41a3114fc0e3ebab6a6390f95))
* `fcli state clear`: Add support for interactive confirmation ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli state clear`: New command to clear state data ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* `fcli state var`: Restructure variable-related commands ([934d607](https://github.com/fortify/fcli/commit/934d607384db589067cd2ede6d8cfab61482d265))
* `fcli tool * install`: Add option to warn instead of fail on digest mismatch (resolves [#251](https://github.com/fortify/fcli/issues/251)) ([08e8e26](https://github.com/fortify/fcli/commit/08e8e2639a8ffd357dbfa2a19301a80da2e98ee8))
* `fcli tool * install`: Add support for interactive confirmation ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli tool * install`: Install latest version known to fcli by default, rather than 'latest' to avoid potential digest mismatches (resolves [#251](https://github.com/fortify/fcli/issues/251)) ([08e8e26](https://github.com/fortify/fcli/commit/08e8e2639a8ffd357dbfa2a19301a80da2e98ee8))
* `fcli tool * install`: Rename `--replace-existing` to `--confirm` ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli tool * list`: Include information on what version will be installed by default (resolves [#251](https://github.com/fortify/fcli/issues/251)) ([08e8e26](https://github.com/fortify/fcli/commit/08e8e2639a8ffd357dbfa2a19301a80da2e98ee8))
* `fcli tool * uninstall`: Add support for interactive confirmation ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* `fcli tool * uninstall`: Rename `--confirm-uninstall` to `--confirm` ([8459b8e](https://github.com/fortify/fcli/commit/8459b8ed2931ccb4bf3d9bf94060b3c7a92a09e1))
* Add `fcli fod release download-fpr` command ([a918cda](https://github.com/fortify/fcli/commit/a918cda7d605127034f2cf300657f5f39df110ae))
* Add `fcli scm github-contributor list` command ([ceb3325](https://github.com/fortify/fcli/commit/ceb332552c5a0e8180c96075003260850dc0d220))
* Add `fcli scm gitlab-contributor list` command ([81d0985](https://github.com/fortify/fcli/commit/81d0985e044eed2369456b13649f46c4d99b69ca))
* Add `fcli ssc appversion refresh-metrics` command (fixes [#335](https://github.com/fortify/fcli/issues/335)) ([04ac595](https://github.com/fortify/fcli/commit/04ac5956b4756da0f4b0f1528336efb18d3d8f6b))
* Add `fcli util all-commands help` command ([ff8ede1](https://github.com/fortify/fcli/commit/ff8ede17beed8218ee9f3d05e54bdac2fc693fe3))
* Add `fcli util crypto` commands ([a85b226](https://github.com/fortify/fcli/commit/a85b22600d89342cf8e8b68ba3af3fd210a063a1))
* Add `fcli util msp-report` commands ([9982985](https://github.com/fortify/fcli/commit/9982985afe73bc445ceca3df5b3a3bde1bb05dcf))
* Add `fcli util ncd-report` commands ([a85b226](https://github.com/fortify/fcli/commit/a85b22600d89342cf8e8b68ba3af3fd210a063a1))
* Add `fcli util sample-data` commands ([4b5215b](https://github.com/fortify/fcli/commit/4b5215b4ee432af7bdf5d838779663cc995f7023))
* Add FCLI_DEFAULT_* environment variable support for all positional parameters (closes [#136](https://github.com/fortify/fcli/issues/136)) ([67fcf85](https://github.com/fortify/fcli/commit/67fcf85867d4c054eb34f4f03cff50cebe80f7a9))
* Add support for resolving default variable property using `::var::` syntax (resolves [#160](https://github.com/fortify/fcli/issues/160)) ([4021d35](https://github.com/fortify/fcli/commit/4021d35e77d698353d00ed03cf2d9733f6dca433))
* added sc-sast sensor list command ([dfc930f](https://github.com/fortify/fcli/commit/dfc930f53bd80326dfdd6b7ab91dd41a037c7ed2))
* added ssc user create command ([7acc319](https://github.com/fortify/fcli/commit/7acc319fa10a7373a59bda7f64439bcbbbea24f5))
* Change `--no-progress` to `--progress &lt;type&gt;` (closes [#305](https://github.com/fortify/fcli/issues/305)) ([1431b49](https://github.com/fortify/fcli/commit/1431b499ee73f5eee9e3e2c149afa8a5d5b5439d))
* Change syntax for referencing variables from `{?var:prop}` to `::var::prop` (resolves [#160](https://github.com/fortify/fcli/issues/160)) ([4021d35](https://github.com/fortify/fcli/commit/4021d35e77d698353d00ed03cf2d9733f6dca433))
* Cleanup: Easiest approach to clean up old configuration and state data is to delete the fcli data directory (usually &lt;user-home&gt;/.fortify/fcli) before you start using this new fcli version ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* Environment: Add support for `FCLI_CONFIG_DIR` and `FCLI_STATE_DIR` environment variables, allowing for example to have a shared config directory and private state directory ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* Environment: Rename `FORTIFY_HOME` and `FCLI_HOME` environment variables to `FORTIFY_DATA_DIR` and `FCLI_DATA_DIR` (closes [#248](https://github.com/fortify/fcli/issues/248)) ([a6d8b36](https://github.com/fortify/fcli/commit/a6d8b361619c168836d09a7692ad4a7008b9de3a))
* FoD: `fcli fod app create-web-app` add option `--auto-required-attrs` to automatically set required attribute values (closes [#311](https://github.com/fortify/fcli/issues/311)) ([c32e604](https://github.com/fortify/fcli/commit/c32e6046c42ed0db147eacf77cc336ca7f0e6b98))
* FoD: Add `--filters-param` option for specifying server-side query ([acb6cf5](https://github.com/fortify/fcli/commit/acb6cf5570a2e2cb8ddd9913752adca51148da74))
* FoD: add `fcli fod scan start-mobile` (implements [#260](https://github.com/fortify/fcli/issues/260)) ([266cf37](https://github.com/fortify/fcli/commit/266cf37ec1feef5e16d76e9bad5bd9f1c2482914))
* FoD: Add `fod sast-scan setup` (implements [#225](https://github.com/fortify/fcli/issues/225)) ([f7d718d](https://github.com/fortify/fcli/commit/f7d718ddacf870d43097960db92f55b91849c626))
* FoD: Add `fod sast-scan setup` (implements [#225](https://github.com/fortify/fcli/issues/225)) ([e556f1e](https://github.com/fortify/fcli/commit/e556f1e027f8adb5f164fc4e67af163e83e6fd6e))
* FoD: Added functionality for user CRUD (implements [#245](https://github.com/fortify/fcli/issues/245)) ([818622a](https://github.com/fortify/fcli/commit/818622acc3050ea9289a45739ef6dffc9832073e))
* FoD: Added functionality for user group CRUD (implements [#246](https://github.com/fortify/fcli/issues/246)) ([818622a](https://github.com/fortify/fcli/commit/818622acc3050ea9289a45739ef6dffc9832073e))
* FoD: Automatically generate server-side query for simple SpEL expressions passed with the `-q`/`--query` option ([acb6cf5](https://github.com/fortify/fcli/commit/acb6cf5570a2e2cb8ddd9913752adca51148da74))
* FoD: refactor `fcli fod app` creation commands (implements [#266](https://github.com/fortify/fcli/issues/266)) ([2b9c453](https://github.com/fortify/fcli/commit/2b9c453153d82911923291f2c5db4c5e573395d4))
* Refactoring & improve `fcli util all-commands` ([c02dba7](https://github.com/fortify/fcli/commit/c02dba78bbd0b8266ac58f7f707d628efb8d0e36))
* Remove support for predefined `?` variables (resolves [#160](https://github.com/fortify/fcli/issues/160)) ([4021d35](https://github.com/fortify/fcli/commit/4021d35e77d698353d00ed03cf2d9733f6dca433))
* Restructure SSC commands ([0076848](https://github.com/fortify/fcli/commit/00768485e32d711d10db8730e7923312267f6543))
* SSC: Add `--q-param` option for specifying server-side query ([497c4e5](https://github.com/fortify/fcli/commit/497c4e5adac97f46dbcebc2ccd908b4e439fca26))
* SSC: Automatically generate server-side query for simple SpEL expressions passed with the `-q`/`--query` option ([497c4e5](https://github.com/fortify/fcli/commit/497c4e5adac97f46dbcebc2ccd908b4e439fca26))
* The .jar version of fcli now requires Java 17 to run (previously Java 11 was required) ([8530999](https://github.com/fortify/fcli/commit/853099986ae66fa43873334abdc5557347e6c523))


### Bug Fixes

* `fcli * rest call`: Fix `--no-transform` behavior ([af5867c](https://github.com/fortify/fcli/commit/af5867cf3bbe0a251aa7ad0fb118a844e0bb4e0f))
* `fcli fod scan-import import-oss` using incorrect endpoint ([0e2fc4f](https://github.com/fortify/fcli/commit/0e2fc4fdd07bbeef9b3a336fd6beb6cb4bbdaffd))
* `fcli sc-dast scan start`: Change short option names to lowercase for consistency (fixes [#325](https://github.com/fortify/fcli/issues/325)) ([164802a](https://github.com/fortify/fcli/commit/164802ae79513995c1c5eee02911a318dd085cda))
* `fcli sc-dast scan start`: Remove --start-urls option; not supported on SC-DAST (fixes [#324](https://github.com/fortify/fcli/issues/324)) ([c06d741](https://github.com/fortify/fcli/commit/c06d741bf10023951c791b2db763ed1bb1410c29))
* `fcli sc-dast session logout`: Remove unused --expire-in option (option is only applicable for login command) ([a0b022b](https://github.com/fortify/fcli/commit/a0b022b73b689a848fc6a0ce6573d905948f598a))
* `fcli sc-sast session logout`: Remove unused --expire-in option (option is only applicable for login command) ([a0b022b](https://github.com/fortify/fcli/commit/a0b022b73b689a848fc6a0ce6573d905948f598a))
* `fcli ssc appversion-artifact download`: Include externalmetadata.xml in current state FPR download by passing arbitrary clientVersion parameter to SSC (fixes [#257](https://github.com/fortify/fcli/issues/257)) ([2694ffe](https://github.com/fortify/fcli/commit/2694ffe0224d85121ea0eaadda64464a0f6f3ff5))
* `fcli ssc report-template generate-answerFile`: Add common options like `--help` ([ec6df34](https://github.com/fortify/fcli/commit/ec6df34f8b0157a01f15d1d10e0f237d249e71dc))
* `fcli ssc report-template generate-answerFile`: Generate proper command output ([ec6df34](https://github.com/fortify/fcli/commit/ec6df34f8b0157a01f15d1d10e0f237d249e71dc))
* `fcli ssc session login`: Show proper error message if no credentials provided (fixes [#326](https://github.com/fortify/fcli/issues/326)) ([30bc902](https://github.com/fortify/fcli/commit/30bc9022869f94a5bf04483a42b872a96779cadf))
* `fcli tool sc-client install`: Add support for latest (22.2.1) version ([38e93eb](https://github.com/fortify/fcli/commit/38e93eb590c15b26090f8b0ae29c761a72db5269))
* `fcli tool sc-client install`: Add support for version 23.1.0 ([a637520](https://github.com/fortify/fcli/commit/a63752018341bbc76ff9e6876c70be57788728c0))
* `fcli tool vuln-exporter install`: Add support for latest (2.0.0) version ([d7ccaea](https://github.com/fortify/fcli/commit/d7ccaea378256d7807020b96499e47bad8aadf3e))
* `fcli tool vuln-exporter install`: Add support for latest (2.0.1) version ([9c34f73](https://github.com/fortify/fcli/commit/9c34f73eb4b7b5474e742d138b908cff6042f438))
* `fcli tool vuln-exporter install`: Add support for latest (2.0.2) version ([e0ce21a](https://github.com/fortify/fcli/commit/e0ce21a851d4f5f85b6ea34cbcbb8a8d18cdff2c))
* `fcli tool`: Update and improve usage instructions (resolves [#251](https://github.com/fortify/fcli/issues/251)) ([08e8e26](https://github.com/fortify/fcli/commit/08e8e2639a8ffd357dbfa2a19301a80da2e98ee8))
* Allow fcli to run if trust store not found ([d955323](https://github.com/fortify/fcli/commit/d95532383132fdb4a1d74c464ca3bd49d541c58d))
* Alternative implementation for b1471ef (fixes [#340](https://github.com/fortify/fcli/issues/340)) ([e4f29c2](https://github.com/fortify/fcli/commit/e4f29c2ee553594f494aa0ac73466d5b37372c72))
* Custom trust store ignored by native binaries (fixes [#253](https://github.com/fortify/fcli/issues/253)) ([a0af875](https://github.com/fortify/fcli/commit/a0af875a2bd511b75863c1c15c8ea1a089e1b4f2))
* Enable auto-completion on all options/parameters taking a file/directory (partial fix for [#336](https://github.com/fortify/fcli/issues/336) & [#351](https://github.com/fortify/fcli/issues/351)) ([b5559d3](https://github.com/fortify/fcli/commit/b5559d32128770240212c1cace9ae91640636f54))
* ensure name option consistency, fixes [#184](https://github.com/fortify/fcli/issues/184) ([f97676f](https://github.com/fortify/fcli/commit/f97676f939e90be4014bfa68a1c1796c7c1de228))
* fix date function null and date handling, fixes [#376](https://github.com/fortify/fcli/issues/376) [#377](https://github.com/fortify/fcli/issues/377) ([11a9c4c](https://github.com/fortify/fcli/commit/11a9c4c0ea953c13092113070c3469c20b8e4e17))
* Fix Micronaut error on fcli -V ([1c4794b](https://github.com/fortify/fcli/commit/1c4794bd5e8f105d33f3b023e7390280ece6028a))
* Fix potential NPE in AbstractToolInstallCommand ([348bd94](https://github.com/fortify/fcli/commit/348bd9474939241d543d8e1ec2b24d98009402f8))
* Fix potential NPE in PagingHelper ([18c3a22](https://github.com/fortify/fcli/commit/18c3a22a7c39c63bb94fb57bdb2f423dbde9a518))
* Fix StringOutOfBoundsException (fixes [#332](https://github.com/fortify/fcli/issues/332)) ([f4d4903](https://github.com/fortify/fcli/commit/f4d4903c071c140b77d42252eec237f4248e6823))
* fixed error on deletion of expired sessions, fixes [#356](https://github.com/fortify/fcli/issues/356) ([bff794d](https://github.com/fortify/fcli/commit/bff794d6e607aed23bc206d26b5388cbf8b79794))
* fixed fcli variables expansion issue, fixes [#394](https://github.com/fortify/fcli/issues/394) ([5ab9221](https://github.com/fortify/fcli/commit/5ab9221aecd3537a0bf9241180201d4e4959e2ee))
* fixed filtering on non-existing values not throwing an error, fixes [#374](https://github.com/fortify/fcli/issues/374) ([4cd2e5d](https://github.com/fortify/fcli/commit/4cd2e5d8254657aef993115ca505aed7348499e5))
* FoD & SC-DAST paging functionality ([af5867c](https://github.com/fortify/fcli/commit/af5867cf3bbe0a251aa7ad0fb118a844e0bb4e0f))
* FoD: `fcli fod app update` remove microservices CRUD (fixes [#282](https://github.com/fortify/fcli/issues/282)) ([edea8d7](https://github.com/fortify/fcli/commit/edea8d7ebf9f40e25c1c01dde4910f7d0ec40f11))
* FoD: `fcli fod lookup-items` (implements [#361](https://github.com/fortify/fcli/issues/361)) ([7409518](https://github.com/fortify/fcli/commit/7409518cb6d3c015b872357b57cd6e15d3a39073))
* FoD: `fcli fod lookup-items` (implements [#361](https://github.com/fortify/fcli/issues/361)) ([6cb9c4e](https://github.com/fortify/fcli/commit/6cb9c4e127389e7cc66b2d7945890a5c410eddee))
* FoD: `fcli fod microservice create --skip-if-exists APP_NAME:MS_NAME` fails (fixes [#319](https://github.com/fortify/fcli/issues/319)) ([adefa05](https://github.com/fortify/fcli/commit/adefa05d28920d773c29bf7401a0ec175a3c66dd))
* FoD: `fcli fod release create ... --microservice=XX` fails (fixes [#320](https://github.com/fortify/fcli/issues/320)) ([adefa05](https://github.com/fortify/fcli/commit/adefa05d28920d773c29bf7401a0ec175a3c66dd))
* FoD: `fcli fod scan sast-scan` 'N/A' columns (fixes [#285](https://github.com/fortify/fcli/issues/285)) ([4de3a53](https://github.com/fortify/fcli/commit/4de3a53f3ae8128404f9a1080d68b131e084dada))
* FoD: `fcli fod scan setup-sast` 'N/A' columns ([531bda2](https://github.com/fortify/fcli/commit/531bda2853ed39370098fcd66fd93eff7f9bb5b9))
* FoD: `fcli fod scan setup-sast` interim fix for NullPointerException (partially fixes [#278](https://github.com/fortify/fcli/issues/278)) ([531bda2](https://github.com/fortify/fcli/commit/531bda2853ed39370098fcd66fd93eff7f9bb5b9))
* FoD: `fcli fod scan start-mobile` not recognising timezones (fixes [#287](https://github.com/fortify/fcli/issues/287)) ([656ca13](https://github.com/fortify/fcli/commit/656ca13ff84fa95fd19cdb9f645dd26bc83465c7))
* FoD: `fcli fod scan start-sast` interim fix for entitlement-id (partially fixes [#279](https://github.com/fortify/fcli/issues/279)) ([4de3a53](https://github.com/fortify/fcli/commit/4de3a53f3ae8128404f9a1080d68b131e084dada))
* FoD: `fcli scan start-mobile` fails if entitlement-id specified (fixes [#286](https://github.com/fortify/fcli/issues/286)) ([74121fa](https://github.com/fortify/fcli/commit/74121faa220d3882f5d102a5155b68b72238e5cc))
* FoD: changes to a number of options to standardize arity (fixes [#268](https://github.com/fortify/fcli/issues/268)) ([4a2412c](https://github.com/fortify/fcli/commit/4a2412c89fb0145ea6febd1bd49c0a452cba0a11))
* FoD: changes to a number of options to standardize arity (fixes [#268](https://github.com/fortify/fcli/issues/268)) ([2b9c453](https://github.com/fortify/fcli/commit/2b9c453153d82911923291f2c5db4c5e573395d4))
* FoD: Fix command usage headers (fixes [#359](https://github.com/fortify/fcli/issues/359)) ([6cb9c4e](https://github.com/fortify/fcli/commit/6cb9c4e127389e7cc66b2d7945890a5c410eddee))
* FoD: Refactor class names to represent commands (resolves [#362](https://github.com/fortify/fcli/issues/362)) ([c0e1781](https://github.com/fortify/fcli/commit/c0e1781d439de0d61af60353d04826a394ddfcbb))
* FoD: refactor scan commands to be under single entity (fixes [#262](https://github.com/fortify/fcli/issues/262)) ([266cf37](https://github.com/fortify/fcli/commit/266cf37ec1feef5e16d76e9bad5bd9f1c2482914))
* FoD: Review `fcli fod app create-*-app` commands (resolves [#367](https://github.com/fortify/fcli/issues/367)) ([34d8a1d](https://github.com/fortify/fcli/commit/34d8a1dfbb4ce19aff42573a5818f458e9bd27f3))
* Improve handling of config changes during single Java/app run ([8eafacd](https://github.com/fortify/fcli/commit/8eafacd48a4f02d7d27245724bf9c0eb0fa8f8e4))
* Improve handling of destination directories contain .. for tool * install commands (fixes [#345](https://github.com/fortify/fcli/issues/345)) ([b5559d3](https://github.com/fortify/fcli/commit/b5559d32128770240212c1cace9ae91640636f54))
* Incorrect behavior for `ssc appversion --embed` (fixes 401) ([0f13ec8](https://github.com/fortify/fcli/commit/0f13ec8d7484dd03fd378de0ec947cd08df20c12))
* Lookup and query values now use case-sensitive matching, to avoid inconsistent behavior with case-sensitive server-side matching and case-insensitive client-side matching (fixes [#125](https://github.com/fortify/fcli/issues/125), fixes [#185](https://github.com/fortify/fcli/issues/185)) ([837791f](https://github.com/fortify/fcli/commit/837791fcece08ca03c3468ec601baab83a71ff0c))
* Missing aliases on some commands, like `ls` on `session list` and `tool list` commands ([640020e](https://github.com/fortify/fcli/commit/640020eabdc59f64ce7b75df1b00231bce7edbe3))
* Output all contents in tree output (fixes [#104](https://github.com/fortify/fcli/issues/104)) ([f4f4f81](https://github.com/fortify/fcli/commit/f4f4f814a3f2acfd832dc25e07b2b86a98a17496))
* Output all contents in tree output (fixes [#104](https://github.com/fortify/fcli/issues/104)) ([0a3de1b](https://github.com/fortify/fcli/commit/0a3de1bc14591aca2cbabe8b181428ad614a4fef))
* Patch for https://github.com/remkop/picocli/issues/2068 (fixes [#336](https://github.com/fortify/fcli/issues/336)) ([071e19f](https://github.com/fortify/fcli/commit/071e19fcf05d7edafbbe0d72f8561d3d1f756948))
* Potential NullPointerException if expression passed to '-o expr=...' returns null ([8508aaf](https://github.com/fortify/fcli/commit/8508aafb6f4b70455407b9e5216dd257297bbccc))
* Properly handle required options in exclusive ArgGroups (fixes [#327](https://github.com/fortify/fcli/issues/327)) ([e25638a](https://github.com/fortify/fcli/commit/e25638aa677370735942319e8d55a4c80c3b466d))
* Re-add fcli-thirdparty.zip to release assets (fixes [#366](https://github.com/fortify/fcli/issues/366)) ([e491d39](https://github.com/fortify/fcli/commit/e491d39ea185b2605c606984d4b9a84c1591d14e))
* Use Controller URL instead of SSC URL for Controller requests (fixes [#353](https://github.com/fortify/fcli/issues/353)) ([3ca3862](https://github.com/fortify/fcli/commit/3ca3862d8501cfbd10cadbee30248a518243dfa3))


### Miscellaneous Chores

* release 1.2.4 ([4f23048](https://github.com/fortify/fcli/commit/4f230489e566f6e647f45b040053026909b6cb58))

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
