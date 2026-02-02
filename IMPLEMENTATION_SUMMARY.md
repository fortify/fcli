# Implementation Summary: Debug Log Collection for CI Systems

## Overview

This implementation adds comprehensive debug log collection features to fcli, enabling consistent logging across multiple CI/CD platforms (GitHub Actions, GitLab CI, Azure DevOps, Bitbucket Pipelines).

## Problem Statement

The goal was to implement features that:
1. Enable debug logging consistently across CI systems
2. Allow integration with CI-specific debug settings
3. Write logs to a known location for easy archiving
4. Keep log collection outside fcli (handled by CI-specific steps)

## Solution

### Core Components

#### 1. CiLogHelper Class
**Location:** `fcli-core/fcli-common/src/main/java/com/fortify/cli/common/log/CiLogHelper.java`

A utility class that provides:
- Detection of debug mode from fcli and CI-specific environment variables
- Log level configuration from environment
- Log file/directory path resolution
- Diagnostic methods for troubleshooting

**Supported Environment Variables:**
- `FCLI_DEBUG` - Enable debug logging (true/1)
- `FCLI_LOG_LEVEL` - Set log level (TRACE/DEBUG/INFO/WARN/ERROR/NONE)
- `FCLI_LOG_FILE` - Explicit path to log file
- `FCLI_LOG_DIR` - Directory for log files (default: current directory)

**Auto-Detected CI Debug Variables:**
- GitHub Actions: `ACTIONS_STEP_DEBUG`, `RUNNER_DEBUG`
- GitLab CI: `CI_DEBUG_TRACE`
- Azure DevOps: `SYSTEM_DEBUG`
- Bitbucket: `BITBUCKET_PIPELINES_DEBUG_MODE`

#### 2. FortifyCLIDynamicInitializer Integration
**Location:** `fcli-core/fcli-app/src/main/java/com/fortify/cli/app/runner/util/FortifyCLIDynamicInitializer.java`

Modified to:
- Check for debug mode from both command-line and environment
- Resolve log file path using CiLogHelper
- Respect log level from environment variables

**Priority Order:**
1. Command-line flags (`--debug`, `--log-level`, `--log-file`) - highest priority
2. fcli environment variables (`FCLI_DEBUG`, `FCLI_LOG_LEVEL`, `FCLI_LOG_FILE`, `FCLI_LOG_DIR`)
3. CI-specific debug variables (auto-detected)
4. Default behavior - lowest priority

#### 3. Updated Help Text
**Location:** `fcli-core/fcli-common/src/main/resources/com/fortify/cli/common/i18n/FortifyCLIMessages.properties`

Updated descriptions for `--debug`, `--log-level`, and `--log-file` options to document environment variable support.

### Testing

#### Automated Tests
**Location:** `fcli-core/fcli-common/src/test/java/com/fortify/cli/common/log/CiLogHelperTest.java`

18 comprehensive tests covering:
- Debug mode detection from FCLI_DEBUG
- Debug mode detection from all CI-specific variables
- Log level parsing and validation
- Log file/directory path resolution
- Default behavior
- Diagnostic methods

**Result:** All tests pass ✅

#### Manual Testing
Verified with actual fcli jar:
- ✅ FCLI_DEBUG environment variable enables debug logging
- ✅ ACTIONS_STEP_DEBUG (GitHub Actions) auto-enables debug
- ✅ CI_DEBUG_TRACE (GitLab CI) auto-enables debug
- ✅ FCLI_LOG_LEVEL=INFO creates INFO-level logs (no DEBUG)
- ✅ FCLI_LOG_FILE creates log at custom path
- ✅ FCLI_LOG_DIR creates fcli.log in specified directory

### Documentation

#### 1. CI Debug Logging Guide
**Location:** `doc-resources/ci-debug-logging.md`

Comprehensive guide covering:
- Environment variable reference
- CI-specific debug variable table
- Priority order
- Examples for all major CI systems
- Log collection best practices
- Security considerations (masking sensitive data)
- Troubleshooting guide
- API reference for CI tool developers

#### 2. GitHub Action v3 Integration Guide
**Location:** `doc-resources/github-action-v3-integration.md`

Specific implementation guide for GitHub Action v3:
- Architecture and separation of concerns
- Complete composite action implementation example
- Usage examples (auto-debug, explicit debug, custom log directories)
- Testing strategies
- Best practices for job summaries and artifact upload
- Migration guide from v2 to v3
- Troubleshooting common issues

#### 3. Documentation Index
**Location:** `doc-resources/README.md`

Quick start guide and documentation structure overview.

## API for CI Integrations

CI tool developers can use the CiLogHelper programmatically:

```java
import com.fortify.cli.common.log.CiLogHelper;

// Check if CI debug is enabled
boolean isDebug = CiLogHelper.isCiDebugEnabled();

// Get the configured log directory
Path logDir = CiLogHelper.getLogDir();

// Get debug configuration source (for diagnostics)
String source = CiLogHelper.getDebugConfigSource();
```

## Usage Example (GitHub Actions)

### Basic Setup
```yaml
jobs:
  fortify-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run fcli scan
        env:
          FCLI_LOG_DIR: ${{ github.workspace }}/logs
        run: |
          fcli ssc session login --url $SSC_URL --token $SSC_TOKEN
          fcli ssc scan start --appversion MyApp:main
          
      - name: Upload debug logs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: fcli-logs
          path: logs/
```

Debug is automatically enabled when:
- User re-runs workflow with "Enable debug logging" option
- `ACTIONS_STEP_DEBUG` secret is set to `true`
- `RUNNER_DEBUG` secret is set to `1`

## Benefits

### For fcli Users
- Consistent debug logging across all CI platforms
- No need to learn different debug mechanisms for each CI system
- Centralized log location for easy artifact collection
- Automatic debug mode when CI debug is enabled

### For CI Integration Developers
- Simple environment variable configuration
- Auto-detection reduces boilerplate
- Clear API for programmatic access
- Comprehensive documentation and examples

### For Troubleshooting
- Standardized log format across CI systems
- Easy to collect and analyze
- Automatic masking of sensitive data
- Diagnostic methods to identify configuration issues

## Security

- Sensitive data (tokens, passwords) automatically masked using existing fcli log masking infrastructure
- Configurable masking levels via `FCLI_LOG_MASK` environment variable
- No new security vulnerabilities introduced (verified with CodeQL)
- Documentation includes security considerations for artifact retention

## Backward Compatibility

- No breaking changes to existing fcli behavior
- Command-line flags take precedence over environment variables
- Existing workflows continue to work unchanged
- New features are opt-in via environment variables

## Future Enhancements

Possible future improvements:
1. Additional CI systems (Jenkins, CircleCI, Travis CI)
2. Structured log output formats (JSON, XML) for automated parsing
3. Integration with CI-specific logging APIs (e.g., GitHub Actions annotations)
4. Log rotation for long-running jobs
5. Real-time log streaming to CI job output

## Files Modified/Added

### Core Implementation
- ✨ NEW: `fcli-core/fcli-common/src/main/java/com/fortify/cli/common/log/CiLogHelper.java`
- 🔧 MODIFIED: `fcli-core/fcli-app/src/main/java/com/fortify/cli/app/runner/util/FortifyCLIDynamicInitializer.java`
- 📝 MODIFIED: `fcli-core/fcli-common/src/main/resources/com/fortify/cli/common/i18n/FortifyCLIMessages.properties`

### Tests
- ✨ NEW: `fcli-core/fcli-common/src/test/java/com/fortify/cli/common/log/CiLogHelperTest.java`

### Documentation
- ✨ NEW: `doc-resources/ci-debug-logging.md`
- ✨ NEW: `doc-resources/github-action-v3-integration.md`
- ✨ NEW: `doc-resources/README.md`

## Review & Security

- ✅ Code review completed - no issues found
- ✅ Security scan (CodeQL) completed - no vulnerabilities
- ✅ All automated tests pass (18/18)
- ✅ Manual testing completed successfully
- ✅ Documentation comprehensive and accurate

## Conclusion

This implementation provides a robust, well-tested, and well-documented solution for debug log collection in CI systems. It follows fcli's design principles, maintains backward compatibility, and provides clear value to both users and CI integration developers.

The separation of concerns (fcli provides logging, CI tools provide collection) ensures that fcli failures don't affect log collection while enabling rich integration with CI-specific features like job summaries and artifact management.
