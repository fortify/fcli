# fcli Action Function Syntax: Design Analysis

**Date:** 2026-02-04  
**Status:** Research & Recommendations  
**Authors:** fcli Team

## Executive Summary

This document analyzes the feasibility and design considerations for introducing function definitions to fcli actions, including integration with the existing MCP server infrastructure. The analysis concludes that **adding embedded JavaScript support** would provide the most significant benefits while maintaining compatibility with existing fcli action syntax.

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Benefits of Function Definitions](#benefits-of-function-definitions)
3. [Language Choice Analysis](#language-choice-analysis)
4. [Integration Architecture](#integration-architecture)
5. [IDE and AI Assistance Comparison](#ide-and-ai-assistance-comparison)
6. [Implementation Recommendations](#implementation-recommendations)
7. [Security Considerations](#security-considerations)
8. [Migration Path](#migration-path)

---

## Current State Analysis

### Existing Action Capabilities

fcli actions currently support:

1. **Declarative Step Execution**
   - REST API calls with pagination
   - fcli command invocation
   - Variable management
   - Conditional logic (`if`, `breakIf`)
   - Iteration (`records.for-each`)

2. **Spring Expression Language (SpEL) Integration**
   - 60+ built-in functions (text, date, CI/CD, Fortify-specific)
   - Variable interpolation: `${varName}`
   - Complex expressions: `${collection.?[expression]}`
   - Custom function registration per module

3. **Reusability Patterns**
   - **Formatters**: Reusable data transformation templates
   - **Nested Steps**: Conditional step grouping
   - **Embedded REST**: Load related data inline
   - **Helper Actions**: Composition via `run.fcli`

4. **MCP Server Support**
   - Actions exposed as MCP tools: `fcli_<module>_action_<action-name>`
   - JSON-RPC transport over stdio
   - Action filtering via `config.mcp: include|exclude`
   - Async job management with progress tracking

### Current Limitations

1. **No Action-Level Functions**
   - Cannot define reusable code blocks within actions
   - No parameterized function calls
   - Limited to SpEL built-in functions

2. **SpEL Constraints**
   - String-based expressions prone to syntax errors
   - Limited IDE support (no autocomplete, syntax highlighting)
   - Complex logic becomes unreadable
   - Difficult debugging (no step-through)

3. **Limited Looping**
   - Only `records.for-each` supported
   - No while/until loops
   - No break/continue in nested contexts

4. **No Cross-Action Composition**
   - Cannot import functions from other actions
   - Full action invocation only via `run.fcli`

### Example: Current SpEL Complexity

```yaml
steps:
  - var.set:
      # Complex nested SpEL expression
      result: ${collection.?[status=='NEW' && severity=='Critical']
              .![{name: issueName, 
                  file: fullFileName + ':' + lineNumber,
                  url: #ssc.issueBrowserUrl(#this, fs)}]}
```

This becomes difficult to read, test, and maintain.

---

## Benefits of Function Definitions

### 1. Improved Code Reusability

**Current:** Duplicate logic across actions
```yaml
# Action 1
- var.set:
    highCriticalIssues: ${issues.?[friority=='Critical' && scanStatus=='NEW']}

# Action 2 (duplicate logic)
- var.set:
    highCriticalIssues: ${issues.?[friority=='Critical' && scanStatus=='NEW']}
```

**With Functions:**
```yaml
functions:
  filterNewCriticalIssues:
    params: [issues]
    returns: ${issues.?[friority=='Critical' && scanStatus=='NEW']}

steps:
  - var.set:
      highCriticalIssues: ${#filterNewCriticalIssues(issues)}
```

### 2. MCP Tool Exposure

Functions could be **directly exposed as MCP tools**, enabling:
- Fine-grained AI assistant integration
- Composable workflows from AI prompts
- Reduced tool invocation overhead

**Example MCP Integration:**
```yaml
functions:
  calculateRiskScore:
    description: "Calculate risk score based on issue severity and count"
    mcp: include
    params:
      - name: issues
        type: array
        description: "Array of issues"
    returns:
      type: number
    body: |
      ${issues.?[friority=='Critical'].size() * 10 + 
        issues.?[friority=='High'].size() * 5}
```

Exposed as: `fcli_ssc_function_calculateRiskScore`

### 3. Better Testing

Functions enable **unit testing** of action logic:
```yaml
tests:
  - function: filterNewCriticalIssues
    input:
      issues: [{friority: 'Critical', scanStatus: 'NEW'}]
    expected: [{friority: 'Critical', scanStatus: 'NEW'}]
```

### 4. Simplified Complex Logic

**Before (SpEL):**
```yaml
${issueList.![
  {issue: issueName,
   location: fullFileName + (lineNumber != null ? ':' + lineNumber : ''),
   url: #ssc.issueBrowserUrl(#this, filterSet),
   priority: friority}
].?[priority == 'Critical' || priority == 'High']}
```

**After (Function):**
```yaml
${#formatCriticalIssues(issueList, filterSet)}
```

---

## Language Choice Analysis

### Option 1: Enhanced YAML with SpEL (Minimal Change)

**Pros:**
- No new dependencies
- Consistent with existing syntax
- Familiar to current users

**Cons:**
- SpEL limitations remain (debugging, IDE support)
- String-based expressions error-prone
- Limited type safety

**Example:**
```yaml
functions:
  calculateScore:
    params: [criticalCount, highCount]
    body: ${criticalCount * 10 + highCount * 5}
```

### Option 2: JavaScript (GraalVM JS) - **RECOMMENDED**

**Pros:**
- ✅ **Ubiquitous language** - most developers know JavaScript
- ✅ **Excellent IDE support** - VSCode, IntelliJ with full autocomplete
- ✅ **Better AI assistance** - AI models trained extensively on JS
- ✅ **Mature ecosystem** - npm packages (with limitations)
- ✅ **GraalVM native image compatible** - via GraalVM JavaScript
- ✅ **Debuggable** - source maps, breakpoints
- ✅ **Type safety** - JSDoc or TypeScript type hints

**Cons:**
- ⚠️ New dependency (~3-4MB GraalVM JS runtime)
- ⚠️ Security sandboxing required
- ⚠️ Learning curve for YAML-only users

**Example:**
```yaml
functions:
  calculateRiskScore:
    language: javascript
    params: [issues]
    body: |
      /**
       * Calculate risk score based on issue severity
       * @param {Array<Issue>} issues - Array of security issues
       * @returns {number} Risk score
       */
      function calculateRiskScore(issues) {
        const criticalCount = issues.filter(i => i.friority === 'Critical').length;
        const highCount = issues.filter(i => i.friority === 'High').length;
        return criticalCount * 10 + highCount * 5;
      }
      return calculateRiskScore(issues);
```

**IDE Experience:**
```javascript
// .fcli-action-types.d.ts (auto-generated type hints)
interface Issue {
  id: string;
  friority: 'Critical' | 'High' | 'Medium' | 'Low';
  scanStatus: 'NEW' | 'UPDATED' | 'REMOVED';
  issueName: string;
  fullFileName: string;
  lineNumber?: number;
}

declare function calculateRiskScore(issues: Issue[]): number;
```

### Option 3: Python (GraalVM Python)

**Pros:**
- Popular in data science/security
- Clean syntax
- Good AI model training

**Cons:**
- Larger runtime (~10MB+)
- GraalVM Python has limitations
- Less universal than JavaScript

### Option 4: Lua (Lightweight Scripting)

**Pros:**
- Minimal footprint (~200KB)
- Fast execution
- Simple syntax

**Cons:**
- Less common - limited developer familiarity
- Poor IDE/AI support
- Smaller ecosystem

### Recommendation Matrix

| Criteria | YAML+SpEL | JavaScript | Python | Lua |
|----------|-----------|------------|--------|-----|
| Developer Familiarity | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| IDE Support | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| AI Assistance | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| GraalVM Native | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Runtime Size | 0MB | 3-4MB | 10MB+ | 200KB |
| Security | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Overall** | ⭐⭐⭐ | **⭐⭐⭐⭐⭐** | ⭐⭐⭐ | ⭐⭐⭐ |

**Winner: JavaScript (GraalVM JS)**

---

## Integration Architecture

### Proposed Function Syntax

```yaml
# Action definition with functions
author: Fortify
usage:
  header: Process security scan results

config:
  rest.target.default: ssc

# Function definitions
functions:
  # Simple SpEL function (backward compatible)
  formatIssueCount:
    params: [count, label]
    returns: ${count + ' ' + label + (count != 1 ? 's' : '')}
  
  # JavaScript function with full capabilities
  analyzeIssues:
    language: javascript  # default: spel
    description: "Analyze issues and return risk assessment"
    mcp: include  # Expose as MCP tool
    params:
      - name: issues
        type: array
        description: "Array of security issues"
      - name: threshold
        type: number
        description: "Risk score threshold"
    returns:
      type: object
      properties:
        riskScore: number
        recommendation: string
    body: |
      function analyzeIssues(issues, threshold) {
        // Access fcli action variables
        const appVersion = fcli.vars.av.name;
        
        // Calculate risk score
        const critical = issues.filter(i => i.friority === 'Critical').length;
        const high = issues.filter(i => i.friority === 'High').length;
        const riskScore = critical * 10 + high * 5;
        
        // Call other functions
        const details = formatIssueDetails(issues);
        
        // Call SpEL functions
        const timestamp = fcli.spel.eval('#now()');
        
        // Execute REST calls
        const appDetails = fcli.rest.get(`/api/v1/projectVersions/${fcli.vars.av.id}`);
        
        // Execute fcli commands
        const issueStats = fcli.exec('ssc issue count --av ' + fcli.vars.av.id);
        
        return {
          riskScore: riskScore,
          recommendation: riskScore > threshold ? 'FAIL' : 'PASS',
          timestamp: timestamp,
          details: details
        };
      }
      
      return analyzeIssues(issues, threshold);

# Use functions in steps
steps:
  - rest.call:
      issues:
        uri: /api/v1/projectVersions/${av.id}/issues
  
  # Call JavaScript function
  - var.set:
      analysis: ${#analyzeIssues(issues.data, 50)}
  
  # Use function result
  - if: ${analysis.recommendation == 'FAIL'}
    throw: "Risk score too high: ${analysis.riskScore}"
  
  - log.info: ${#formatIssueCount(issues.count, 'issue')}
```

### JavaScript Runtime Integration

#### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      fcli Action Runner                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           ActionStepProcessor (Java)                  │  │
│  │  - Processes YAML steps                               │  │
│  │  - Evaluates SpEL expressions                         │  │
│  │  - Delegates to FunctionExecutor for JS functions     │  │
│  └──────────┬───────────────────────────────────────────┘  │
│             │                                                │
│  ┌──────────▼───────────────────────────────────────────┐  │
│  │        ActionFunctionExecutor (Java)                  │  │
│  │  - Manages function registry                          │  │
│  │  - Routes to SpEL or JS runtime                       │  │
│  │  - Handles parameter binding                          │  │
│  └──────────┬───────────────────────────────────────────┘  │
│             │                                                │
│       ┌─────┴─────┐                                         │
│       │           │                                         │
│  ┌────▼────┐ ┌───▼──────────────────────────────────────┐ │
│  │  SpEL   │ │   GraalVM JavaScript Runtime             │ │
│  │ Runtime │ │  ┌────────────────────────────────────┐  │ │
│  └─────────┘ │  │  Sandboxed JS Context              │  │ │
│              │  │  - Limited Java access              │  │ │
│              │  │  - fcli API bridge                  │  │ │
│              │  │  - Resource limits (CPU, memory)    │  │ │
│              │  └────────────────────────────────────┘  │ │
│              │                                            │ │
│              │  ┌────────────────────────────────────┐  │ │
│              │  │  fcli JavaScript API                │  │ │
│              │  │  - fcli.vars (get/set variables)    │  │ │
│              │  │  - fcli.rest (REST operations)      │  │ │
│              │  │  - fcli.exec (fcli commands)        │  │ │
│              │  │  - fcli.spel (SpEL evaluation)      │  │ │
│              │  │  - fcli.log (logging)               │  │ │
│              │  └────────────────────────────────────┘  │ │
│              └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### JavaScript Bridge API

```javascript
// fcli JavaScript API (automatically available in function context)

/**
 * Access and modify action variables
 */
fcli.vars = {
  // Get variable value
  get(name) { /* ... */ },
  
  // Set variable value  
  set(name, value) { /* ... */ },
  
  // Remove variable
  remove(name) { /* ... */ },
  
  // Direct property access
  av: { /* current av variable */ },
  cli: { /* CLI options */ }
};

/**
 * Execute REST calls
 */
fcli.rest = {
  // Execute REST request
  call(options) { /* ... */ },
  
  // Convenience methods
  get(uri, options) { /* ... */ },
  post(uri, body, options) { /* ... */ },
  put(uri, body, options) { /* ... */ },
  delete(uri, options) { /* ... */ }
};

/**
 * Execute fcli commands
 */
fcli.exec = function(command, options) {
  // Returns: { exitCode, stdout, stderr, records }
};

/**
 * Evaluate SpEL expressions
 */
fcli.spel = {
  eval(expression) { /* ... */ }
};

/**
 * Logging
 */
fcli.log = {
  info(message) { /* ... */ },
  warn(message) { /* ... */ },
  debug(message) { /* ... */ },
  progress(message) { /* ... */ }
};

/**
 * Utility functions
 */
fcli.util = {
  // JSON operations
  json: {
    parse(str) { /* ... */ },
    stringify(obj) { /* ... */ }
  },
  
  // Call other action functions
  call(functionName, ...args) { /* ... */ }
};
```

#### Example: Complex Function

```yaml
functions:
  generateComplianceReport:
    language: javascript
    description: "Generate compliance report with policy violations"
    params:
      - name: issues
        type: array
      - name: policies
        type: object
    returns:
      type: object
    body: |
      function generateReport(issues, policies) {
        // Group issues by category
        const grouped = issues.reduce((acc, issue) => {
          const cat = issue.engineCategory || 'Unknown';
          if (!acc[cat]) acc[cat] = [];
          acc[cat].push(issue);
          return acc;
        }, {});
        
        // Check policy violations
        const violations = [];
        for (const [category, categoryIssues] of Object.entries(grouped)) {
          const criticalCount = categoryIssues.filter(i => 
            i.friority === 'Critical'
          ).length;
          
          const policy = policies[category];
          if (policy && criticalCount > policy.maxCritical) {
            violations.push({
              category: category,
              criticalCount: criticalCount,
              allowed: policy.maxCritical,
              excess: criticalCount - policy.maxCritical
            });
          }
        }
        
        // Get additional context from REST API
        const appVersion = fcli.vars.get('av');
        const metadata = fcli.rest.get(`/api/v1/projectVersions/${appVersion.id}`);
        
        // Format report
        const report = {
          applicationVersion: appVersion.name,
          scanDate: fcli.spel.eval('#now()'),
          totalIssues: issues.length,
          byCategory: Object.keys(grouped).map(cat => ({
            category: cat,
            count: grouped[cat].length,
            critical: grouped[cat].filter(i => i.friority === 'Critical').length
          })),
          violations: violations,
          compliant: violations.length === 0,
          metadata: metadata
        };
        
        // Log progress
        fcli.log.info(`Analyzed ${issues.length} issues across ${Object.keys(grouped).length} categories`);
        
        return report;
      }
      
      return generateReport(issues, policies);
```

### SpEL Function Registration

JavaScript functions automatically registered as SpEL functions:

```yaml
functions:
  myJsFunction:
    language: javascript
    params: [arg1, arg2]
    body: |
      return arg1 + arg2;

steps:
  # Call from SpEL context
  - var.set:
      result: ${#myJsFunction(10, 20)}  # Returns 30
```

### MCP Tool Exposure

Functions with `mcp: include` automatically exposed:

```yaml
functions:
  searchVulnerabilities:
    language: javascript
    description: "Search for vulnerabilities matching criteria"
    mcp: include
    params:
      - name: criteria
        type: object
        properties:
          severity: {type: string, enum: [Critical, High, Medium, Low]}
          status: {type: string, enum: [NEW, UPDATED, REMOVED]}
    body: |
      const { severity, status } = criteria;
      const issues = fcli.rest.get(`/api/v1/projectVersions/${fcli.vars.av.id}/issues`, {
        query: {
          filter: `severity:${severity}+status:${status}`
        }
      });
      return issues.data;
```

Accessible from MCP client:
```javascript
// MCP client invocation
const result = await mcpClient.callTool(
  'fcli_ssc_function_searchVulnerabilities',
  {
    criteria: {
      severity: 'Critical',
      status: 'NEW'
    }
  }
);
```

---

## IDE and AI Assistance Comparison

### Current State: SpEL in YAML

**IDE Support:**
- ❌ No autocomplete for SpEL functions
- ❌ No syntax error detection
- ❌ No inline documentation
- ⚠️ Basic YAML validation only

**AI Assistance:**
- ⚠️ Limited training data for fcli-specific SpEL
- ⚠️ Cannot suggest fcli-specific functions
- ⚠️ No context about variable types

**Example AI Prompt:**
> "Filter issues by critical severity and new status"

**Current AI Output:**
```yaml
# AI struggles with SpEL syntax
- var.set:
    filtered: ${issues.filter(i => i.severity == 'Critical' && i.status == 'NEW')}
    # ❌ Wrong: SpEL doesn't use arrow functions
```

### Proposed: JavaScript Functions

**IDE Support:**
- ✅ Full IntelliSense/autocomplete
- ✅ Real-time syntax checking
- ✅ Inline documentation (JSDoc)
- ✅ Type checking (TypeScript definitions)
- ✅ Refactoring support (rename, extract)
- ✅ Debugging (breakpoints, watch)

**AI Assistance:**
- ✅ Extensive training on JavaScript
- ✅ Understands idiomatic patterns
- ✅ Type inference from JSDoc/TypeScript
- ✅ Better code suggestions

**Example AI Prompt:**
> "Filter issues by critical severity and new status"

**AI Output with JS:**
```javascript
/**
 * Filter issues by severity and status
 * @param {Issue[]} issues - Array of issues
 * @returns {Issue[]} Filtered issues
 */
function filterCriticalNewIssues(issues) {
  return issues.filter(issue => 
    issue.friority === 'Critical' && issue.scanStatus === 'NEW'
  );
}
// ✅ Correct, idiomatic JavaScript
```

### Comparison Table

| Feature | SpEL in YAML | JavaScript |
|---------|--------------|------------|
| Syntax Highlighting | Basic YAML | Full JS |
| Autocomplete | None | Full |
| Error Detection | Runtime only | Real-time |
| Debugging | Print statements | Breakpoints |
| Type Safety | None | JSDoc/TypeScript |
| Refactoring | Manual | Automated |
| AI Code Quality | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Learning Resources | Limited | Abundant |

### VSCode Extension Opportunity

```json
// .vscode/settings.json (auto-generated)
{
  "yaml.schemas": {
    "https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev-2.x.json": "*.yaml"
  },
  "files.associations": {
    "*.fcli-function.js": "javascript"
  },
  "[javascript]": {
    "editor.suggest.snippets": "top"
  }
}
```

Code snippets:
```javascript
// fcli.code-snippets
{
  "fcli Function": {
    "prefix": "fcli-function",
    "body": [
      "/**",
      " * ${1:Function description}",
      " * @param {${2:type}} ${3:paramName} - ${4:description}",
      " * @returns {${5:type}} ${6:return description}",
      " */",
      "function ${7:functionName}(${3}) {",
      "  ${8:// Implementation}",
      "  return ${9:result};",
      "}",
      "",
      "return ${7}(${3});"
    ]
  }
}
```

---

## Implementation Recommendations

### Phase 1: Foundation (v2.8.0)

**Goals:** Enable basic JavaScript function definitions

**Tasks:**
1. Add GraalVM JavaScript dependency (~3-4MB)
   ```kotlin
   // build.gradle.kts
   implementation("org.graalvm.polyglot:js:24.0.0")
   ```

2. Extend action schema with `functions` section
   ```json
   {
     "functions": {
       "type": "object",
       "patternProperties": {
         "^[a-zA-Z][a-zA-Z0-9_]*$": {
           "type": "object",
           "properties": {
             "language": {"enum": ["spel", "javascript"]},
             "params": {"type": "array"},
             "returns": {"type": "object"},
             "body": {"type": "string"}
           }
         }
       }
     }
   }
   ```

3. Implement `ActionFunctionExecutor`
   ```java
   public class ActionFunctionExecutor {
       private final Context jsContext;
       private final ActionRunnerContext actionContext;
       
       public Object executeFunction(String name, Object... args) {
           var function = actionContext.getAction().getFunctions().get(name);
           if (function.getLanguage() == FunctionLanguage.JAVASCRIPT) {
               return executeJavaScript(function, args);
           } else {
               return executeSpel(function, args);
           }
       }
   }
   ```

4. JavaScript sandbox with security policies
   ```java
   Context jsContext = Context.newBuilder("js")
       .allowHostAccess(HostAccess.newBuilder()
           .allowAccessAnnotatedBy(JsFunctionAccess.class)
           .build())
       .allowIO(IOAccess.NONE) // No file system access
       .resourceLimits(ResourceLimits.newBuilder()
           .maxStatements(100000)
           .build())
       .build();
   ```

5. Basic `fcli` JavaScript API
   ```java
   @JsFunctionAccess
   public class FcliJsApi {
       private final ActionRunnerContext context;
       
       @JsFunctionAccess
       public Object getVar(String name) { /* ... */ }
       
       @JsFunctionAccess
       public void setVar(String name, Object value) { /* ... */ }
       
       @JsFunctionAccess
       public void logInfo(String message) { /* ... */ }
   }
   ```

**Deliverables:**
- ✅ JavaScript function execution
- ✅ Basic fcli API (`fcli.vars`, `fcli.log`)
- ✅ SpEL function registration
- ✅ Unit tests
- ✅ Documentation

### Phase 2: Integration (v2.9.0)

**Goals:** Full fcli API and MCP integration

**Tasks:**
1. Complete `fcli` JavaScript API
   - `fcli.rest` - REST operations
   - `fcli.exec` - fcli command execution
   - `fcli.spel` - SpEL evaluation

2. MCP function exposure
   - Auto-register functions as MCP tools
   - Parameter schema generation
   - Result formatting

3. IDE support tooling
   - TypeScript definition generator
   - VSCode extension (syntax, snippets)
   - Action validator CLI

4. Migration utilities
   - SpEL-to-JS converter (for common patterns)
   - Action analyzer (complexity metrics)

**Deliverables:**
- ✅ Full JavaScript API
- ✅ MCP tool exposure
- ✅ IDE support
- ✅ Migration guide

### Phase 3: Advanced Features (v2.10.0)

**Goals:** Testing, debugging, and optimization

**Tasks:**
1. Function testing framework
   ```yaml
   functions:
     myFunction:
       params: [input]
       body: |
         return input * 2;
   
   tests:
     myFunction:
       - input: [5]
         expected: 10
       - input: [0]
         expected: 0
   ```

2. Source maps for debugging
3. Performance profiling
4. Function composition (higher-order functions)
5. Async function support

**Deliverables:**
- ✅ Testing framework
- ✅ Debugging tools
- ✅ Performance optimization
- ✅ Best practices guide

### Backward Compatibility

**Guarantees:**
1. Existing actions work unchanged
2. SpEL remains fully supported
3. No breaking changes to action schema
4. Opt-in JavaScript adoption

**Example Migration:**

**Before (SpEL):**
```yaml
steps:
  - var.set:
      critical: ${issues.?[friority=='Critical'].size()}
      high: ${issues.?[friority=='High'].size()}
      score: ${critical * 10 + high * 5}
```

**After (JavaScript, optional):**
```yaml
functions:
  calculateRiskScore:
    language: javascript
    params: [issues]
    body: |
      const critical = issues.filter(i => i.friority === 'Critical').length;
      const high = issues.filter(i => i.friority === 'High').length;
      return critical * 10 + high * 5;

steps:
  - var.set:
      score: ${#calculateRiskScore(issues)}
```

---

## Security Considerations

### Sandbox Requirements

1. **No File System Access**
   - Prevent reading/writing files
   - Exception: Approved paths via fcli API

2. **No Network Access**
   - All REST calls through `fcli.rest` API
   - Controlled authentication/authorization

3. **No Java Reflection**
   - Limited host access via whitelist
   - No arbitrary Java class instantiation

4. **Resource Limits**
   - CPU: Max execution time (default: 30s)
   - Memory: Max heap per function (default: 64MB)
   - Statement count: Prevent infinite loops

5. **No Process Execution**
   - fcli commands only via `fcli.exec` API
   - Validated command whitelist

### Implementation

```java
public class SecureJavaScriptExecutor {
    private static final long MAX_EXECUTION_TIME_MS = 30000;
    private static final long MAX_MEMORY_BYTES = 64 * 1024 * 1024;
    
    public Object execute(ActionFunction function, Object... args) {
        Context.Builder builder = Context.newBuilder("js")
            // Restrict host access
            .allowHostAccess(HostAccess.newBuilder()
                .allowAccessAnnotatedBy(JsFunctionAccess.class)
                .denyAccess(Class.class)
                .build())
            // No IO
            .allowIO(IOAccess.NONE)
            // Resource limits
            .resourceLimits(ResourceLimits.newBuilder()
                .statementLimit(100000, null)
                .build())
            // Timeout
            .option("engine.WarnInterpreterOnly", "false");
        
        try (Context context = builder.build()) {
            // Set timeout
            context.getEngine().getInstruments()
                .get("cpuTimeInstrument")
                .lookup(CPUTimeInstrument.class)
                .setLimit(MAX_EXECUTION_TIME_MS);
            
            // Execute with memory limit
            return executeWithMemoryLimit(context, function, args);
        }
    }
    
    private Object executeWithMemoryLimit(Context ctx, ActionFunction fn, Object... args) {
        // Inject fcli API
        ctx.getBindings("js").putMember("fcli", createFcliApi());
        
        // Execute function
        Value result = ctx.eval("js", fn.getBody());
        return convertToJava(result);
    }
}
```

### Security Audit Checklist

- [ ] Sandbox prevents file system access
- [ ] Sandbox prevents network access (except fcli.rest)
- [ ] Resource limits enforced (CPU, memory)
- [ ] No reflection or class loading
- [ ] fcli API validates all inputs
- [ ] Sensitive data masked in logs
- [ ] Function execution audited
- [ ] Malicious code detection (static analysis)

---

## Migration Path

### For Action Developers

1. **Identify Complex SpEL**
   - Search for nested `${...}` expressions
   - Look for repeated patterns
   - Find difficult-to-read logic

2. **Extract to Functions**
   - Start with utility functions
   - Convert data transformations
   - Consolidate duplicates

3. **Add Tests**
   - Unit test functions
   - Integration test actions
   - Verify MCP exposure

### For fcli Maintainers

1. **Documentation**
   - JavaScript API reference
   - Migration examples
   - Best practices guide
   - Security guidelines

2. **Tooling**
   - VSCode extension
   - Type definition generator
   - SpEL-to-JS converter (limited)
   - Action linter

3. **Education**
   - Blog posts
   - Video tutorials
   - Sample actions
   - Community support

---

## Alternative Approaches Considered

### 1. Pure SpEL Enhancement

**Considered:** Improve SpEL with better IDE support, function definitions

**Rejected Because:**
- SpEL fundamentally string-based (limits tooling)
- Small ecosystem (IDE plugins unlikely)
- Spring Expression Language not designed for complex logic

### 2. Domain-Specific Language (DSL)

**Considered:** Custom fcli action DSL with native syntax

**Rejected Because:**
- High development cost (parser, runtime, tooling)
- Learning curve for users
- Limited AI training data
- Reinventing the wheel

### 3. JVM Languages (Kotlin, Groovy)

**Considered:** Leverage JVM scripting

**Rejected Because:**
- Larger runtime overhead
- Security concerns (full JVM access)
- Less universal than JavaScript
- Compilation complexity

### 4. WebAssembly (WASM)

**Considered:** Compile functions to WASM

**Rejected Because:**
- Complex toolchain
- Limited debugging
- Overkill for action scripts
- GraalVM JS sufficient

---

## Conclusion

### Summary of Recommendations

1. **✅ Add JavaScript Function Support**
   - Use GraalVM JavaScript for embedded execution
   - Maintain backward compatibility with existing SpEL-based actions
   - Provide comprehensive `fcli` JavaScript API

2. **✅ Expose Functions as MCP Tools**
   - Enable direct function invocation from AI assistants
   - Auto-generate tool schemas from function definitions
   - Support both actions and functions as MCP tools

3. **✅ Enhance IDE Support**
   - Generate TypeScript definitions for type hints
   - Create VSCode extension for fcli actions
   - Provide code snippets and templates

4. **✅ Prioritize Security**
   - Strict JavaScript sandbox with resource limits
   - Controlled fcli API with input validation
   - Audit logging for function execution

5. **✅ Phased Implementation**
   - Phase 1 (v2.8.0): Basic JavaScript functions
   - Phase 2 (v2.9.0): Full API and MCP integration
   - Phase 3 (v2.10.0): Testing and advanced features

### Expected Benefits

- **Improved Reusability:** Define once, use across actions and MCP tools
- **Better Developer Experience:** IDE autocomplete, syntax checking, debugging
- **Enhanced AI Assistance:** AI models excel at JavaScript
- **MCP Integration:** Fine-grained tool exposure for AI assistants
- **Maintainability:** Testable, documented, version-controlled functions

### Next Steps

1. **Stakeholder Review:** Gather feedback on design proposal
2. **Prototype:** Build proof-of-concept (Phase 1 minimal implementation)
3. **Security Review:** Validate sandbox implementation
4. **Community Feedback:** Share with fcli users for input
5. **Implementation:** Execute phased rollout per recommendations

---

## Appendix A: Complete Example

### Before: Traditional Action (SpEL Only)

```yaml
# yaml-language-server: $schema=https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev-2.x.json

author: Fortify
usage:
  header: Check security policy compliance

config:
  rest.target.default: ssc

cli.options:
  appversion:
    names: --appversion, --av
    description: "Application version"
  max-critical:
    names: --max-critical
    type: int
    default: 0
  max-high:
    names: --max-high
    type: int
    default: 10

steps:
  - var.set:
      av: ${#ssc.appVersion(cli.appversion)}
      fs: ${#ssc.filterSet(av, null)}
  
  - rest.call:
      issues:
        uri: /api/v1/projectVersions/${av.id}/issues
        records.for-each:
          record.var-name: issue
          do:
            - var.set:
                criticalIssues..: {if: "${issue.friority=='Critical'}"}
                highIssues..: {if: "${issue.friority=='High'}"}
  
  - var.set:
      criticalCount: ${criticalIssues==null ? 0 : criticalIssues.size()}
      highCount: ${highIssues==null ? 0 : highIssues.size()}
      
  - check:
      criticalCheck:
        display-name: "Critical issues: ${criticalCount} (max: ${cli['max-critical']})"
        fail.if: ${criticalCount > cli['max-critical']}
      highCheck:
        display-name: "High issues: ${highCount} (max: ${cli['max-high']})"
        fail.if: ${highCount > cli['max-high']}
```

### After: Action with JavaScript Functions

```yaml
# yaml-language-server: $schema=https://fortify.github.io/fcli/schemas/action/fcli-action-schema-dev-2.x.json

author: Fortify
usage:
  header: Check security policy compliance

config:
  rest.target.default: ssc

cli.options:
  appversion:
    names: --appversion, --av
    description: "Application version"
  policy:
    names: --policy
    description: "Policy name (uses embedded policies if not specified)"

functions:
  # Define reusable policy check logic
  checkPolicy:
    language: javascript
    description: "Validate issues against security policy"
    mcp: include  # Expose as MCP tool for AI assistants
    params:
      - name: issues
        type: array
        description: "Array of security issues"
      - name: policyName
        type: string
        description: "Policy name to check"
    returns:
      type: object
      properties:
        passed: {type: boolean}
        violations: {type: array}
        summary: {type: string}
    body: |
      /**
       * Check issues against policy
       * @param {Issue[]} issues - Security issues
       * @param {string} policyName - Policy to apply
       * @returns {PolicyResult} Policy check result
       */
      function checkPolicy(issues, policyName) {
        // Built-in policies
        const policies = {
          strict: { maxCritical: 0, maxHigh: 5 },
          standard: { maxCritical: 5, maxHigh: 20 },
          relaxed: { maxCritical: 10, maxHigh: 50 }
        };
        
        const policy = policies[policyName] || policies.standard;
        
        // Count by severity
        const counts = issues.reduce((acc, issue) => {
          const severity = issue.friority || 'Unknown';
          acc[severity] = (acc[severity] || 0) + 1;
          return acc;
        }, {});
        
        const criticalCount = counts.Critical || 0;
        const highCount = counts.High || 0;
        
        // Check violations
        const violations = [];
        if (criticalCount > policy.maxCritical) {
          violations.push({
            severity: 'Critical',
            count: criticalCount,
            allowed: policy.maxCritical,
            excess: criticalCount - policy.maxCritical
          });
        }
        if (highCount > policy.maxHigh) {
          violations.push({
            severity: 'High',
            count: highCount,
            allowed: policy.maxHigh,
            excess: highCount - policy.maxHigh
          });
        }
        
        // Generate summary
        const passed = violations.length === 0;
        const summary = passed 
          ? `Policy "${policyName}" passed: ${criticalCount} critical, ${highCount} high`
          : `Policy "${policyName}" failed with ${violations.length} violation(s)`;
        
        // Log result
        fcli.log.info(summary);
        
        return {
          passed: passed,
          violations: violations,
          summary: summary,
          counts: counts,
          policy: policy
        };
      }
      
      return checkPolicy(issues, policyName);

steps:
  - var.set:
      av: ${#ssc.appVersion(cli.appversion)}
      fs: ${#ssc.filterSet(av, null)}
  
  - rest.call:
      issues:
        uri: /api/v1/projectVersions/${av.id}/issues
  
  # Call JavaScript function
  - var.set:
      policyResult: ${#checkPolicy(issues.data, cli.policy ?: 'standard')}
  
  # Use function result
  - if: ${!policyResult.passed}
    throw: ${policyResult.summary}
  
  - log.info: "✅ ${policyResult.summary}"
```

### MCP Tool Usage

From AI assistant:
```
User: Check if my SSC app has any critical security issues

AI: I'll check your application's security policy compliance.

[Calls MCP tool: fcli_ssc_function_checkPolicy]
{
  "issues": [...fetched from SSC...],
  "policyName": "strict"
}

Result: Policy check passed. Your application has 0 critical and 3 high severity issues, which complies with the strict policy (max 0 critical, max 5 high).
```

---

## Appendix B: References

### GraalVM JavaScript
- **Official Docs:** https://www.graalvm.org/latest/reference-manual/js/
- **Native Image Support:** https://www.graalvm.org/latest/reference-manual/js/NativeImage/
- **Polyglot API:** https://www.graalvm.org/latest/reference-manual/embed-languages/

### Model Context Protocol (MCP)
- **Specification:** https://spec.modelcontextprotocol.io/
- **SDK:** https://github.com/modelcontextprotocol/java-sdk
- **Documentation:** https://modelcontextprotocol.io/

### Related Projects
- **GitHub Actions:** JavaScript-based workflow actions
- **Ansible:** Python-based automation with module system
- **Terraform:** HCL with embedded functions
- **Jsonnet:** Data templating language with functions

### Security Resources
- **OWASP Secure Coding:** https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/
- **GraalVM Security:** https://www.graalvm.org/latest/security-guide/
- **JavaScript Sandboxing:** Research on VM-based sandboxing techniques

---

**Document End**
