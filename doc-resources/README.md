# fcli CI/CD Integration Documentation

This directory contains documentation for integrating fcli with various CI/CD systems.

## Documents

### [ci-debug-logging.md](ci-debug-logging.md)
Comprehensive guide for enabling and collecting debug logs from fcli across all CI systems:
- Environment variables (`FCLI_DEBUG`, `FCLI_LOG_LEVEL`, `FCLI_LOG_FILE`, `FCLI_LOG_DIR`)
- Auto-detection of CI-specific debug flags
- Examples for GitHub Actions, GitLab CI, Azure DevOps, Bitbucket Pipelines
- Best practices for log collection and artifact management
- Security considerations (masking sensitive data)

### [github-action-v3-integration.md](github-action-v3-integration.md)
Specific implementation guide for GitHub Action v3:
- Architecture and separation of concerns
- Complete composite action implementation example
- Usage examples (auto-debug, explicit debug, custom log directories)
- Testing strategies with act and GitHub Actions
- Best practices for job summaries and artifact upload
- Migration guide from v2 to v3
- Troubleshooting common issues

## Quick Start

### For CI Users

If you're running fcli in a CI environment and want debug logging:

1. **Set environment variable:**
   ```bash
   export FCLI_LOG_DIR=/path/to/logs
   ```

2. **Enable debug (choose one):**
   - Use CI-specific debug mode (GitHub Actions: enable debug logging for workflow run)
   - Set `FCLI_DEBUG=true` environment variable
   - Set `FCLI_LOG_LEVEL=TRACE` for most verbose logging

3. **Archive logs after execution:**
   ```bash
   # Example for GitHub Actions
   - uses: actions/upload-artifact@v4
     if: always()
     with:
       name: fcli-logs
       path: /path/to/logs/
   ```

### For Action/Integration Developers

If you're building a GitHub Action, GitLab template, or similar CI integration:

1. Read [ci-debug-logging.md](ci-debug-logging.md) for general principles
2. Review [github-action-v3-integration.md](github-action-v3-integration.md) for GitHub-specific implementation
3. Adapt the patterns to your CI system (GitLab, Azure DevOps, etc.)

## Key Features

### Auto-Detection
fcli automatically enables debug logging when running in CI with debug mode enabled:
- GitHub Actions: `ACTIONS_STEP_DEBUG` or `RUNNER_DEBUG`
- GitLab CI: `CI_DEBUG_TRACE`
- Azure DevOps: `SYSTEM_DEBUG`
- Bitbucket: `BITBUCKET_PIPELINES_DEBUG_MODE`

### Centralized Logs
All fcli logs go to a single directory specified by `FCLI_LOG_DIR`:
```
$FCLI_LOG_DIR/
  └── fcli.log         # Main fcli log file
```

CI integrations should also collect related files:
- `fortify-scan-*.log` - ScanCentral SAST logs
- `scancentral-*.log` - ScanCentral Client logs
- `sensor-*.log` - Sensor logs
- `*.fpr` - Scan results (optional)

### Priority Order
1. Command-line flags (`--debug`, `--log-level`, `--log-file`) - highest priority
2. fcli environment variables (`FCLI_DEBUG`, `FCLI_LOG_LEVEL`, etc.)
3. CI-specific debug variables (auto-detected)
4. Default behavior - lowest priority

## Environment Variables Reference

| Variable | Purpose | Example Values |
|----------|---------|----------------|
| `FCLI_DEBUG` | Enable debug logging | `true`, `1` |
| `FCLI_LOG_LEVEL` | Set explicit log level | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `NONE` |
| `FCLI_LOG_FILE` | Explicit log file path | `/logs/fcli.log` |
| `FCLI_LOG_DIR` | Directory for log files | `/logs` |
| `FCLI_LOG_MASK` | Sensitive data masking level | `high`, `medium`, `low`, `none` |

## Support

For issues or questions:
1. Check the troubleshooting section in the relevant documentation
2. Review fcli logs (with `FCLI_LOG_LEVEL=TRACE`)
3. Open an issue at https://github.com/fortify/fcli/issues

## Contributing

When adding support for new CI systems:
1. Follow the patterns in existing documentation
2. Test with the CI system's debug mode
3. Include artifact upload examples
4. Document any CI-specific considerations
5. Add examples to [ci-debug-logging.md](ci-debug-logging.md)
