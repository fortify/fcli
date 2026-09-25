# fcli Action Functions: Executive Summary

**Date:** 2026-02-04  
**Full Analysis:** See [action-functions-design-analysis.md](action-functions-design-analysis.md)

## Problem

Should fcli introduce function definitions to actions that can:
- Be called within action steps
- Be exposed as MCP/JSON-RPC functions
- Use JavaScript or other languages vs. current SpEL syntax

## Recommendation

✅ **Add JavaScript function support using GraalVM JavaScript**

## Why JavaScript?

| Criterion | SpEL | JavaScript | Python | Lua |
|-----------|------|------------|--------|-----|
| Developer Familiarity | ⭐⭐⭐ | **⭐⭐⭐⭐⭐** | ⭐⭐⭐⭐ | ⭐⭐ |
| IDE Support | ⭐⭐ | **⭐⭐⭐⭐⭐** | ⭐⭐⭐⭐ | ⭐⭐ |
| AI Assistance | ⭐⭐ | **⭐⭐⭐⭐⭐** | ⭐⭐⭐⭐ | ⭐⭐ |
| Runtime Size | 0MB | 3-4MB | 10MB+ | 200KB |

## Key Benefits

1. **Improved Reusability**
   ```yaml
   functions:
     calculateRiskScore:
       language: javascript
       params: [issues]
       body: |
         return issues.filter(i => i.friority === 'Critical').length * 10;
   ```

2. **MCP Tool Exposure**
   ```yaml
   functions:
     searchVulnerabilities:
       mcp: include  # Expose to AI assistants
       body: |
         // AI can call this directly
   ```

3. **Better IDE Support**
   - Full autocomplete and IntelliSense
   - Real-time syntax checking
   - Debugging with breakpoints
   - Type hints via JSDoc/TypeScript

4. **Enhanced AI Assistance**
   - AI models trained extensively on JavaScript
   - Better code suggestions and generation
   - Idiomatic patterns

## Example: Before & After

### Before (SpEL only)
```yaml
steps:
  - var.set:
      critical: ${issues.?[friority=='Critical'].size()}
      high: ${issues.?[friority=='High'].size()}
      score: ${critical * 10 + high * 5}
```

### After (JavaScript function)
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

## JavaScript Bridge API

Functions get access to fcli through a comprehensive API:

```javascript
// Access variables
fcli.vars.get('av');
fcli.vars.set('result', value);

// Execute REST calls
fcli.rest.get('/api/v1/issues');
fcli.rest.post('/api/v1/scan', body);

// Run fcli commands
fcli.exec('ssc issue count --av myapp');

// Evaluate SpEL
fcli.spel.eval('#now()');

// Logging
fcli.log.info('Processing complete');
```

## Security

- **Sandboxed execution** - No file system, network, or reflection access
- **Resource limits** - Max CPU time (30s), memory (64MB), statements
- **Controlled API** - All external access via validated fcli API
- **Audit logging** - Function execution tracked

## Implementation Roadmap

### Phase 1 (v2.8.0) - Foundation
- Basic JavaScript execution
- Security sandbox
- fcli API: `vars`, `log`
- SpEL function registration

### Phase 2 (v2.9.0) - Integration
- Full fcli API: `rest`, `exec`, `spel`
- MCP tool exposure
- IDE support (TypeScript definitions, VSCode extension)
- Migration utilities

### Phase 3 (v2.10.0) - Advanced
- Testing framework
- Debugging tools
- Performance optimization
- Best practices guide

## Backward Compatibility

✅ **Guaranteed:**
- Existing actions work unchanged
- SpEL fully supported
- No breaking changes
- Opt-in JavaScript adoption

## Alternative Approaches Considered

❌ **Pure SpEL Enhancement** - Fundamentally limited by string-based nature  
❌ **Custom DSL** - High development cost, learning curve  
❌ **JVM Languages** - Security concerns, less universal  
❌ **WebAssembly** - Overkill for action scripts  

## Next Steps

1. **Stakeholder Review** - Gather feedback on design
2. **Prototype** - Build proof-of-concept
3. **Security Review** - Validate sandbox implementation
4. **Community Feedback** - Share with fcli users
5. **Implementation** - Execute phased rollout

## Full Details

For comprehensive analysis including:
- Complete architecture diagrams
- Detailed security considerations
- MCP integration specifics
- Migration strategies
- Complete code examples

See: [action-functions-design-analysis.md](action-functions-design-analysis.md) (1,367 lines)

---

## Questions?

- **GitHub Issues:** https://github.com/fortify/fcli/issues
- **Community:** https://community.opentext.com/cybersec/fortify/
- **Documentation:** https://fortify.github.io/fcli/
