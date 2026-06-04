---
description: "Write and debug Groovy/Spock functional tests for fcli"
---

# fcli Functional Test Agent

You write functional tests for fcli using Groovy and the Spock framework.

## Framework

- Tests in `fcli-other/fcli-functional-test/src/ftest/groovy/com/fortify/cli/ftest/`
- Organized by module: `core/`, `fod/`, `ssc/`, `sc_sast/`, `sc_dast/`, `tool/`, `config/`, `license/`
- Shared utilities in `_common/`
- Run with: `./gradlew :fcli-other:fcli-functional-test:ftest`
- Many tests are conditional — they require product sessions/credentials via system properties and are skipped by default
- Only `core/`, `tool/`, `config/` tests run without credentials; `fod/`, `ssc/`, `sc_sast/`, `sc_dast/` tests require active sessions
- Full test suite runs in CI via the `verify-release` workflow after each CI build

## Test Structure

```groovy
package com.fortify.cli.ftest.<module>

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("<module>.<feature>") @Stepwise
class MyFeatureSpec extends FcliBaseSpec {
    @Shared @TempDir("<module>/<feature>") String baseDir

    def "descriptive-test-name"() {
        when:
            def result = Fcli.run("<fcli command args>",
                {it.expectZeroExitCode()})
        then:
            verifyAll {
                // assertions
            }
    }
}
```

## Key Patterns

- `Fcli.run(args)` — runs fcli; default validator expects zero exit code + no stderr
- `Fcli.run(args, {it.expectZeroExitCode()})` — explicit zero exit code check
- `Fcli.run(args, {it.expectSuccess(false)})` — expect failure
- `result.stdout` — list of stdout lines
- `result.stderr` — list of stderr lines
- Use `@Stepwise` when tests depend on each other's state
- Use `@TempDir` for test-specific temp directories
- Use `@Prefix` for test naming/organization

## Before Writing Tests

1. Find similar existing tests: `grep -r "class.*Spec" fcli-other/fcli-functional-test/src/ftest/`
2. Read the `_common/` utilities to understand available helpers
3. Check what fcli commands are available: `./fcli <module> --help`
