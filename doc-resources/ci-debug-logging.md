# fcli CI Debug Logging

This document describes how to enable and collect debug logs from fcli in various CI/CD environments.

## Overview

fcli provides built-in support for CI-aware debug logging that:
- Automatically detects when running in a CI environment
- Respects CI-specific debug flags (e.g., `ACTIONS_STEP_DEBUG` for GitHub Actions)
- Allows configuration via environment variables for consistent behavior across CI systems
- Enables centralized log collection to a known directory for easy artifact archiving

## Environment Variables

### fcli-Specific Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `FCLI_DEBUG` | Enable debug logging | `true` or `1` |
| `FCLI_LOG_LEVEL` | Set log level explicitly | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `NONE` |
| `FCLI_LOG_FILE` | Explicit path to log file | `/logs/fcli.log` |
| `FCLI_LOG_DIR` | Directory for log files | `/logs` (creates `fcli.log` in this directory) |

### CI-Specific Debug Variables (Auto-detected)

fcli automatically enables debug logging when any of these CI-specific debug variables are set:

| CI System | Variable | How to Enable |
|-----------|----------|---------------|
| **GitHub Actions** | `ACTIONS_STEP_DEBUG` | Add secret `ACTIONS_STEP_DEBUG` = `true` |
| **GitHub Actions** | `RUNNER_DEBUG` | Add secret `RUNNER_DEBUG` = `1` |
| **GitLab CI** | `CI_DEBUG_TRACE` | Set `CI_DEBUG_TRACE: "true"` in `.gitlab-ci.yml` |
| **Azure DevOps** | `SYSTEM_DEBUG` | Set variable `System.Debug` = `true` |
| **Bitbucket Pipelines** | `BITBUCKET_PIPELINES_DEBUG_MODE` | Enable debug mode in pipeline settings |

## Priority Order

When multiple configuration methods are specified, fcli uses this priority order:

1. Command-line flags (`--debug`, `--log-level`, `--log-file`) - **highest priority**
2. fcli environment variables (`FCLI_DEBUG`, `FCLI_LOG_LEVEL`, `FCLI_LOG_FILE`, `FCLI_LOG_DIR`)
3. CI-specific debug variables (auto-detected)
4. Default behavior - **lowest priority**

## CI Integration Examples

### GitHub Actions

#### Basic Setup with Auto-Debug

Enable debug mode for a workflow run by setting the `ACTIONS_STEP_DEBUG` secret:

```yaml
name: Fortify Scan
on: [push]

jobs:
  fortify-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # Debug automatically enabled when ACTIONS_STEP_DEBUG secret is set
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

#### Explicit Debug Configuration

```yaml
name: Fortify Scan with Debug
on: [push]

jobs:
  fortify-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run fcli scan with debug
        env:
          FCLI_DEBUG: true
          FCLI_LOG_LEVEL: TRACE
          FCLI_LOG_DIR: ${{ github.workspace }}/fcli-logs
        run: |
          fcli ssc session login --url $SSC_URL --token $SSC_TOKEN
          fcli ssc scan start --appversion MyApp:main
          
      - name: Upload fcli logs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: fcli-debug-logs
          path: |
            fcli-logs/
            **/fortify-scan-*.log
            **/scancentral-*.log
```

### GitLab CI

```yaml
variables:
  FCLI_LOG_DIR: "${CI_PROJECT_DIR}/logs"

fortify_scan:
  stage: test
  variables:
    CI_DEBUG_TRACE: "true"  # Auto-enables fcli debug logging
  script:
    - fcli ssc session login --url $SSC_URL --token $SSC_TOKEN
    - fcli ssc scan start --appversion MyApp:main
  artifacts:
    when: always
    paths:
      - logs/
      - "**/*scan*.log"
    expire_in: 1 week
```

### Azure DevOps

```yaml
trigger:
  - main

pool:
  vmImage: 'ubuntu-latest'

variables:
  FCLI_LOG_DIR: $(Build.ArtifactStagingDirectory)/logs
  System.Debug: true  # Auto-enables fcli debug logging

steps:
- checkout: self

- script: |
    fcli ssc session login --url $(SSC_URL) --token $(SSC_TOKEN)
    fcli ssc scan start --appversion MyApp:main
  displayName: 'Run Fortify Scan'

- task: PublishBuildArtifacts@1
  condition: always()
  inputs:
    pathToPublish: '$(Build.ArtifactStagingDirectory)/logs'
    artifactName: 'fcli-logs'
```

### Bitbucket Pipelines

```yaml
pipelines:
  default:
    - step:
        name: Fortify Scan
        script:
          - export FCLI_LOG_DIR="${BITBUCKET_CLONE_DIR}/logs"
          # Debug auto-enabled when pipeline debug mode is on
          - fcli ssc session login --url $SSC_URL --token $SSC_TOKEN
          - fcli ssc scan start --appversion MyApp:main
        artifacts:
          - logs/**
```

## Log Collection Best Practices

### 1. Centralize Log Directory

Always set `FCLI_LOG_DIR` to a known location for easy artifact collection:

```bash
export FCLI_LOG_DIR=/tmp/fcli-logs
```

### 2. Collect All Related Logs

When collecting fcli logs, also archive related scan artifacts:

- `fcli.log` - Main fcli log file
- `fortify-scan-*.log` - ScanCentral SAST scan logs
- `scancentral-*.log` - ScanCentral Client logs
- `sensor-*.log` - Sensor logs (if applicable)
- `*.fpr` - Scan result files (optional)
- Package files created during scanning

### 3. Use Conditional Upload

Always upload logs even when the scan fails:

**GitHub Actions:**
```yaml
- uses: actions/upload-artifact@v4
  if: always()
  with:
    name: fcli-logs
    path: logs/
```

**GitLab CI:**
```yaml
artifacts:
  when: always
  paths:
    - logs/
```

### 4. Set Appropriate Retention

Configure artifact retention based on your needs:

**GitHub Actions:**
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: fcli-logs
    path: logs/
    retention-days: 7
```

**GitLab CI:**
```yaml
artifacts:
  when: always
  paths:
    - logs/
  expire_in: 1 week
```

## Troubleshooting

### Debug Mode Not Activating

1. Check that the CI-specific variable is actually set:
   ```bash
   env | grep -E "ACTIONS_STEP_DEBUG|RUNNER_DEBUG|CI_DEBUG_TRACE|SYSTEM_DEBUG|BITBUCKET_PIPELINES_DEBUG_MODE"
   ```

2. Verify fcli environment variables:
   ```bash
   env | grep FCLI_
   ```

3. Explicitly enable debug mode:
   ```bash
   export FCLI_DEBUG=true
   export FCLI_LOG_LEVEL=TRACE
   ```

### Logs Not Being Created

1. Verify the log directory exists and is writable:
   ```bash
   mkdir -p "$FCLI_LOG_DIR"
   chmod 755 "$FCLI_LOG_DIR"
   ```

2. Check that logging is enabled (not set to NONE):
   ```bash
   # This disables logging - avoid unless intentional
   export FCLI_LOG_LEVEL=NONE
   ```

3. Use explicit log file path:
   ```bash
   export FCLI_LOG_FILE="$PWD/fcli-debug.log"
   ```

### Finding Log Files

Use the `CiLogHelper.getLogDir()` method programmatically, or search for log files:

```bash
# Find all fcli-related logs
find . -name "fcli.log" -o -name "*scan*.log" -o -name "sensor*.log"
```

## Security Considerations

### Sensitive Data Masking

fcli automatically masks sensitive data in logs (tokens, passwords, etc.) based on the `--log-mask` level (default: `medium`). 

To increase masking in CI environments:
```bash
export FCLI_LOG_MASK=high
```

Or via command line:
```bash
fcli --log-mask high ssc session login ...
```

### Log Retention

Debug logs may contain:
- API request/response details
- File paths and directory structures
- Environment information

Configure appropriate retention policies for your security requirements.

## Advanced Usage

### Multiple fcli Invocations

When running multiple fcli commands in sequence, logs append to the same file by default:

```bash
# All commands log to the same file
export FCLI_LOG_DIR=/logs
fcli ssc session login --url $URL --token $TOKEN
fcli ssc appversion get MyApp:main
fcli ssc scan start --appversion MyApp:main
```

### Per-Command Log Files

To create separate log files for each command:

```bash
fcli --log-file /logs/login.log ssc session login ...
fcli --log-file /logs/scan.log ssc scan start ...
```

### Conditional Debug Logging

Enable debug only for specific commands:

```bash
# Normal logging
fcli ssc appversion list

# Debug logging for problematic command
fcli --debug --log-level TRACE ssc scan start --appversion MyApp:main
```

## API for CI Tools

CI tool developers can use the `CiLogHelper` class programmatically:

```java
import com.fortify.cli.common.log.CiLogHelper;

// Check if CI debug is enabled
boolean isDebug = CiLogHelper.isCiDebugEnabled();

// Get the configured log directory
Path logDir = CiLogHelper.getLogDir();

// Get debug configuration source (for diagnostics)
String source = CiLogHelper.getDebugConfigSource();
```
