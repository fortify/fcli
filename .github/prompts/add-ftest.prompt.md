---
description: 'Scaffold a functional test spec for fcli'
---

# Add Functional Test

Create a new Spock functional test for an fcli command.

## Information Needed

1. **Module:** Which module to test? (core, fod, ssc, sc_sast, sc_dast, tool, config, license)
2. **Feature:** What feature/command to test?
3. **Commands:** Which fcli commands to exercise?

## Steps

1. **Find similar tests:**
   ```bash
   find fcli-other/fcli-functional-test/src/ftest/groovy -name "*Spec.groovy" -path "*/<module>/*"
   ```

2. **Read _common utilities** for available helpers:
   ```bash
   ls fcli-other/fcli-functional-test/src/ftest/groovy/com/fortify/cli/ftest/_common/
   ```

3. **Create test spec** at:
   `fcli-other/fcli-functional-test/src/ftest/groovy/com/fortify/cli/ftest/<module>/<Feature>Spec.groovy`

4. **Follow the pattern:**
   - Extend `FcliBaseSpec`
   - Use `@Prefix("<module>.<feature>")` and `@Stepwise`
   - Use `Fcli.run()` for command execution
   - Use `verifyAll {}` for assertions
   - Test both success and expected failure cases

5. **Run:** `./gradlew :fcli-other:fcli-functional-test:ftest`
