---
description: 'Scaffold a new picocli command in fcli'
---

# Add New Command

Create a new fcli picocli command following established patterns.

## Information Needed

1. **Module:** Which product module? (fod, ssc, sc-sast, sc-dast, tool, util, ai-assist, etc.)
2. **Command path:** Full command path (e.g., `fcli ssc appversion-artifact list`)
3. **Output:** Does it produce output? What columns?

## Steps

1. **Find similar commands** in the target module:
   ```bash
   find fcli-core/fcli-<module>/src/main/java -name "*Command.java" | head -20
   ```

2. **Create leaf command class** following the pattern:
   - Extend `Abstract<Product>OutputCommand` or similar
   - Use `OutputHelperMixins.<Type>` for command name + output config
   - Use `@Mixin` for shared options
   - If any parent container commands don't exist yet, create and register them too

3. **Add messages** to `*Messages.properties`:
   - Usage header: `fcli.<path>.usage.header`
   - Option descriptions
   - Table column headers (if output command)

4. **Register** as subcommand in parent container command

5. **Validate:**
   - `get_errors`
   - `./gradlew :fcli-core:fcli-<module>:test` (runs `FortifyCLITest`)
   - `./gradlew build`
