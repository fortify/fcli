# Replace Fully Qualified Class Names with Imports

## Objective
Find all fully qualified class names (FQCNs) used in Java code (not in import statements or package declarations) and replace them with simple class names after adding appropriate import statements.

**Note:** Only update files within the `com.fortify.cli` packages. Do not modify third-party or external code.

## Search Strategy

1. **Initial Search**: Use regex search to find potential FQCNs:
   ```regex
   ^(?!package )(?!import ).*\b(com|org|java|javax|lombok)(\.[a-z][a-z0-9_]*)+\.[A-Z][a-zA-Z0-9]*
   ```
   - Search in: `**/com/fortify/cli/**/*.java`
   - This pattern uses negative lookahead to exclude lines starting with "package " or "import "
   - It finds lines that contain FQCNs in actual code (not import/package statements)

2. **Filter Results**: The search may still include some false positives like:
   - Static import statements (ignore these)
   - Comments containing FQCNs (can be ignored or fixed)
   - Actual FQCNs in code (these need fixing)

## Common FQCN Patterns to Replace

### Java Standard Library
- `java.util.jar.JarFile` → `JarFile`
- `java.util.jar.Manifest` → `Manifest`
- `java.util.jar.Attributes` → `Attributes`
- `java.util.function.Predicate` → `Predicate`
- `java.util.Optional` → `Optional`

### Third-party Libraries
- `picocli.CommandLine.Model.CommandSpec` → `CommandLine.Model.CommandSpec` (after importing `picocli.CommandLine`)
- `picocli.CommandLine.Command` → `CommandLine.Command` (after importing `picocli.CommandLine`)
- `com.fasterxml.jackson.core.type.TypeReference` → `TypeReference`
- `com.fasterxml.jackson.databind.JsonNode` → `JsonNode`

### Lombok Annotations
- `lombok.Builder` → `Builder`
- `lombok.Data` → `Data`
- `lombok.Getter` → `Getter`
- `lombok.Setter` → `Setter`
- `lombok.NoArgsConstructor` → `NoArgsConstructor`
- `lombok.AllArgsConstructor` → `AllArgsConstructor`

### Project Classes
- `com.fortify.cli.common.util.FileUtils` → `FileUtils` (if not in same package)
- Package-specific classes should use simple names with imports

## Replacement Process

For each FQCN found in actual code (not imports/packages):

1. **Check Import Section**: Read the import section to see if the simple name is already imported
2. **Check for Conflicts**: Verify no conflicting simple class name exists from a different package
3. **Add Import**: Add the import statement in the appropriate location (keep imports sorted)
4. **Replace FQCN**: Replace the fully qualified name with the simple name

### Example Transformation

**Before:**
```java
import java.io.File;
import java.nio.file.Path;

public class Example {
    public void method(File file) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(file)) {
            java.util.jar.Manifest manifest = jar.getManifest();
            java.util.jar.Attributes attrs = manifest.getMainAttributes();
        }
    }
}
```

**After:**
```java
import java.io.File;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Example {
    public void method(File file) {
        try (JarFile jar = new JarFile(file)) {
            Manifest manifest = jar.getManifest();
            Attributes attrs = manifest.getMainAttributes();
        }
    }
}
```

## Special Cases

### Nested Classes
When replacing nested class references like `picocli.CommandLine.Model.CommandSpec`:
1. Import the outer class: `import picocli.CommandLine;`
2. Use qualified reference: `CommandLine.Model.CommandSpec`

### Generic Type References
For anonymous inner class instances like `new TypeReference<ArrayList<String>>() {}`:
1. Import the type: `import com.fasterxml.jackson.core.type.TypeReference;`
2. Use simple name: `new TypeReference<ArrayList<String>>() {}`

### Same Package References
If the FQCN refers to a class in the same package, just use the simple name without adding an import.

## Verification Steps

1. **Check Errors**: Use `get_errors` tool to check for compilation errors
2. **Build Project**: Run `./gradlew build -x test` to verify everything compiles
3. **Review Changes**: Ensure imports are added in the correct location and properly sorted

## Notes

- Follow the project's import ordering conventions (java.* first, then javax.*, then third-party, then project imports)
- Use explicit imports; avoid wildcard imports (`import java.util.*`)
- Remove any unused imports after refactoring
- Keep related imports together for readability
