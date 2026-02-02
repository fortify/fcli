# GitHub Action v3 Integration Guide

This document provides specific guidance for integrating fcli debug logging features into the GitHub Action v3 (https://github.com/fortify/github-action/tree/feat/fcli-ci).

## Overview

The GitHub Action v3 should leverage fcli's built-in CI debug logging features to:
1. Automatically enable debug logging when GitHub Actions debug mode is active
2. Centralize all logs to a predictable location
3. Provide easy artifact upload for troubleshooting
4. Integrate with GitHub Actions job summaries

## Architecture

### Separation of Concerns

As outlined in the requirements, debug log collection should remain **outside of fcli** to ensure:
- fcli failures don't affect log collection
- Integration with CI-specific functionality (like job summaries)
- Flexibility for different CI systems

### fcli Provides

1. **Auto-detection of GitHub Actions debug mode**
   - Respects `ACTIONS_STEP_DEBUG` and `RUNNER_DEBUG` secrets
   - Automatically enables TRACE-level logging when detected

2. **Configurable log output location**
   - `FCLI_LOG_DIR` environment variable
   - `CiLogHelper.getLogDir()` API for programmatic access

3. **Standardized logging format**
   - Structured log output with timestamps, log levels, and masking

### GitHub Action Provides

1. **Log directory setup**
   - Create and manage log directory before fcli execution
   - Set `FCLI_LOG_DIR` appropriately

2. **Artifact collection**
   - Archive logs even on failure
   - Include all related scan artifacts (ScanCentral logs, sensor logs, etc.)

3. **Job summary integration**
   - Parse and summarize key information from logs
   - Link to uploaded artifacts

## Implementation Example

### Action Structure (Composite Action)

```yaml
name: 'Fortify Scan'
description: 'Run Fortify scans with fcli'

inputs:
  debug:
    description: 'Enable debug logging (auto-detected from GitHub Actions debug mode)'
    required: false
    default: 'auto'

runs:
  using: 'composite'
  steps:
    # Step 1: Setup log directory
    - name: Setup logging
      shell: bash
      run: |
        LOG_DIR="${{ github.workspace }}/.fortify/logs"
        mkdir -p "$LOG_DIR"
        echo "FCLI_LOG_DIR=$LOG_DIR" >> $GITHUB_ENV
        echo "LOG_DIR=$LOG_DIR" >> $GITHUB_ENV
        
        # Auto-detect or explicit debug mode
        if [ "${{ inputs.debug }}" = "true" ]; then
          echo "FCLI_DEBUG=true" >> $GITHUB_ENV
          echo "Debug logging explicitly enabled"
        elif [ "${{ inputs.debug }}" = "auto" ]; then
          if [ "${{ secrets.ACTIONS_STEP_DEBUG }}" = "true" ] || [ "${{ runner.debug }}" = "1" ]; then
            echo "Debug logging auto-enabled from GitHub Actions debug mode"
          fi
        fi
    
    # Step 2: Run fcli commands
    - name: Run Fortify scan
      shell: bash
      run: |
        # fcli automatically:
        # - Detects ACTIONS_STEP_DEBUG/RUNNER_DEBUG
        # - Creates fcli.log in $FCLI_LOG_DIR
        # - Logs at TRACE level if debug enabled
        
        fcli ssc session login --url "${{ env.SSC_URL }}" --token "${{ secrets.SSC_TOKEN }}"
        fcli ssc scan start --appversion "${{ inputs.appversion }}"
        fcli ssc session logout
    
    # Step 3: Collect all logs (always run, even on failure)
    - name: Collect debug logs
      if: always()
      shell: bash
      run: |
        # Collect fcli logs
        echo "Collecting logs from $LOG_DIR"
        ls -la "$LOG_DIR" || true
        
        # Find and copy additional scan-related logs
        find . -type f \( \
          -name "fcli*.log" \
          -o -name "fortify-scan*.log" \
          -o -name "scancentral*.log" \
          -o -name "sensor*.log" \
        \) -exec cp {} "$LOG_DIR/" \; 2>/dev/null || true
        
        # List collected files
        echo "Files collected:"
        ls -lh "$LOG_DIR/"
    
    # Step 4: Generate job summary
    - name: Generate debug summary
      if: always()
      shell: bash
      run: |
        {
          echo "## Debug Log Collection"
          echo ""
          echo "Debug logging was enabled for this run."
          echo ""
          echo "### Collected Files"
          echo ""
          echo "\`\`\`"
          ls -lh "$LOG_DIR/" 2>/dev/null || echo "No log files found"
          echo "\`\`\`"
          echo ""
          echo "### Log Location"
          echo "- FCLI_LOG_DIR: \`$LOG_DIR\`"
          echo "- GitHub Workspace: \`${{ github.workspace }}\`"
          echo ""
          
          # Check for errors in fcli.log
          if [ -f "$LOG_DIR/fcli.log" ]; then
            ERROR_COUNT=$(grep -c "ERROR" "$LOG_DIR/fcli.log" 2>/dev/null || echo 0)
            WARN_COUNT=$(grep -c "WARN" "$LOG_DIR/fcli.log" 2>/dev/null || echo 0)
            echo "### Log Summary"
            echo "- Errors: $ERROR_COUNT"
            echo "- Warnings: $WARN_COUNT"
            echo ""
            
            if [ "$ERROR_COUNT" -gt 0 ]; then
              echo "### Recent Errors"
              echo ""
              echo "\`\`\`"
              grep "ERROR" "$LOG_DIR/fcli.log" | tail -10
              echo "\`\`\`"
              echo ""
            fi
          fi
          
          echo "Download the 'fortify-debug-logs' artifact for complete logs."
        } >> $GITHUB_STEP_SUMMARY
    
    # Step 5: Upload artifacts
    - name: Upload debug logs
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: fortify-debug-logs
        path: |
          ${{ env.LOG_DIR }}/
          **/*.fpr
        retention-days: 7
        if-no-files-found: warn
```

## Usage Examples

### Basic Usage (Auto-Debug)

```yaml
name: Fortify Scan

on: [push]

jobs:
  fortify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Fortify Scan
        uses: fortify/github-action@v3
        with:
          ssc-url: ${{ secrets.SSC_URL }}
          ssc-token: ${{ secrets.SSC_TOKEN }}
          # Debug auto-enabled when ACTIONS_STEP_DEBUG secret is set
```

To enable debug for this workflow run:
1. Go to Actions tab
2. Select the workflow run
3. Click "Re-run jobs" → "Enable debug logging"

### Explicit Debug Mode

```yaml
name: Fortify Scan with Debug

on: [push]

jobs:
  fortify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Fortify Scan
        uses: fortify/github-action@v3
        with:
          ssc-url: ${{ secrets.SSC_URL }}
          ssc-token: ${{ secrets.SSC_TOKEN }}
          debug: true  # Always enable debug
```

### Custom Log Directory

```yaml
name: Fortify Scan with Custom Logs

on: [push]

jobs:
  fortify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup custom log directory
        run: |
          mkdir -p ${{ runner.temp }}/fortify-logs
          echo "FCLI_LOG_DIR=${{ runner.temp }}/fortify-logs" >> $GITHUB_ENV
      
      - name: Fortify Scan
        uses: fortify/github-action@v3
        with:
          ssc-url: ${{ secrets.SSC_URL }}
          ssc-token: ${{ secrets.SSC_TOKEN }}
      
      - name: Upload logs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: fortify-logs
          path: ${{ runner.temp }}/fortify-logs/
```

## Testing Strategy

### Local Testing with act

Test the action locally using [act](https://github.com/nektos/act):

```bash
# Test with debug mode
export FCLI_DEBUG=true
export FCLI_LOG_DIR=/tmp/act-logs
mkdir -p /tmp/act-logs

# Run with act
act push -s ACTIONS_STEP_DEBUG=true
```

### GitHub Actions Testing

1. Create a test workflow with debug enabled
2. Verify logs are collected:
   - Check job summary for log information
   - Download artifacts
   - Inspect fcli.log for TRACE-level entries

3. Test failure scenarios:
   - Intentionally cause fcli to fail
   - Verify logs are still uploaded
   - Check that failure info appears in job summary

## Best Practices

### 1. Always Upload Logs on Failure

```yaml
- name: Upload logs
  if: always()  # Critical: run even when previous steps fail
  uses: actions/upload-artifact@v4
```

### 2. Include Comprehensive Artifacts

```yaml
path: |
  ${{ env.LOG_DIR }}/
  **/*.fpr
  **/*scan*.log
  **/sensor*.log
  **/scancentral*.log
```

### 3. Use Job Summaries

Parse and summarize key information rather than forcing users to download artifacts:

```bash
ERROR_COUNT=$(grep -c "ERROR" "$LOG_DIR/fcli.log" || echo 0)
echo "- Errors: $ERROR_COUNT" >> $GITHUB_STEP_SUMMARY
```

### 4. Set Appropriate Retention

```yaml
retention-days: 7  # Balance storage costs with troubleshooting needs
```

### 5. Handle Missing Files Gracefully

```yaml
if-no-files-found: warn  # Don't fail if logs are missing
```

## Security Considerations

### Sensitive Data in Logs

fcli automatically masks sensitive data (tokens, passwords) in logs using configurable masking levels:

```yaml
env:
  FCLI_LOG_MASK: high  # More aggressive masking for public repos
```

Masking levels:
- `high` - Maximum masking (recommended for public repos)
- `medium` - Balanced masking (default)
- `low` - Minimal masking (for internal debugging)
- `none` - No masking (use with extreme caution)

### Artifact Access Control

Be aware that artifacts are accessible to:
- Repository collaborators
- Anyone with a direct artifact link (if made public)

For sensitive environments:
1. Use private repositories
2. Set short retention periods
3. Use `FCLI_LOG_MASK=high`
4. Review logs before sharing externally

## Troubleshooting

### Logs Not Being Created

1. **Check environment variable is set:**
   ```yaml
   - name: Debug env vars
     run: |
       echo "FCLI_LOG_DIR=$FCLI_LOG_DIR"
       echo "FCLI_DEBUG=$FCLI_DEBUG"
   ```

2. **Verify directory exists:**
   ```yaml
   - name: Setup logs
     run: |
       mkdir -p "$FCLI_LOG_DIR"
       ls -la "$FCLI_LOG_DIR"
   ```

3. **Check fcli version:**
   ```bash
   fcli --version
   # Need version 3.x or later for CI debug logging features
   ```

### Debug Mode Not Activating

1. **Check GitHub Actions debug secrets:**
   - Repository Settings → Secrets → Actions
   - Add `ACTIONS_STEP_DEBUG` = `true`

2. **Manually enable for single run:**
   - Actions → Select workflow → Re-run → Enable debug logging

3. **Use explicit debug flag:**
   ```yaml
   env:
     FCLI_DEBUG: true
   ```

### Artifacts Not Uploading

1. **Check path exists:**
   ```yaml
   - name: Verify logs exist
     if: always()
     run: ls -R ${{ env.LOG_DIR }}
   ```

2. **Use absolute paths:**
   ```yaml
   path: ${{ github.workspace }}/.fortify/logs/
   ```

3. **Check for empty directories:**
   ```yaml
   if-no-files-found: warn  # Continue even if empty
   ```

## Migration from v2

If migrating from GitHub Action v2:

### Before (v2)
```yaml
- name: Manual log collection
  run: |
    mkdir logs
    cp fcli.log logs/ || true
    find . -name "*.log" -exec cp {} logs/ \;
```

### After (v3)
```yaml
# Set environment variable once
env:
  FCLI_LOG_DIR: ${{ github.workspace }}/logs

# fcli automatically creates logs in FCLI_LOG_DIR
# No manual collection needed
```

Benefits:
- Automatic debug mode detection
- Consistent log locations
- Built-in CI integration
- Less boilerplate code

## References

- [fcli CI Debug Logging Documentation](../ci-debug-logging.md)
- [GitHub Actions: Enabling debug logging](https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows/enabling-debug-logging)
- [GitHub Actions: Storing workflow data as artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)
- [GitHub Actions: Adding a job summary](https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#adding-a-job-summary)
