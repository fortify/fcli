# fcli Documentation Resources

This directory contains various design documents, research papers, and technical resources for fcli development.

## Documents

### [action-functions-design-analysis.md](action-functions-design-analysis.md)
**Status:** Research & Recommendations  
**Date:** 2026-02-04

Comprehensive design analysis for introducing function definitions to fcli actions, including:

- **Problem Statement:** Evaluation of adding reusable function syntax to fcli YAML actions
- **Language Recommendation:** JavaScript (GraalVM) for embedded scripting
- **Key Benefits:**
  - Improved code reusability across actions
  - Direct MCP tool exposure for AI assistant integration
  - Better IDE support (autocomplete, debugging, type checking)
  - Enhanced AI code assistance
- **Integration:** Architecture for JavaScript bridge API with fcli variables, REST calls, and commands
- **Security:** Comprehensive sandboxing requirements and implementation
- **Implementation:** 3-phase rollout plan (v2.8.0 - v2.10.0)

**Recommendation:** Add JavaScript function support while maintaining backward compatibility with existing SpEL-based actions.

### Other Documents

- **repo-intro.md** - Repository introduction text
- **repo-usage.md** - Usage information
- **repo-devinfo.md** - Developer information
- **repo-resources.md** - Resource links
- **template-values.md** - Template configuration values

## Related Resources

- **Action Documentation:** https://fortify.github.io/fcli/actions/
- **Action Schema:** https://fortify.github.io/fcli/schemas/action/
- **MCP Server Guide:** See `fcli util mcp-server --help`
- **Action Examples:** `fcli-*/src/main/resources/com/fortify/cli/*/actions/zip/*.yaml`

## Contributing

For questions or discussions about these design documents, please:

1. Open an issue in the [fcli GitHub repository](https://github.com/fortify/fcli/issues)
2. Join the [Fortify Community](https://community.opentext.com/cybersec/fortify/)
3. Contact the fcli maintainers

---

*Last updated: 2026-02-04*
