/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.tool

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * Functional tests for 'fcli tool env init' command.
 * 
 * Tests cover both option-based and environment variable-based configuration
 * for the most common CI integration scenarios: sc-client, debricked-cli, and fcli.
 * 
 * Test isolation: Uses @TempDir to create isolated installation directories for each test run,
 * preventing interference from existing installations or previous test runs.
 */
@Prefix("tool.env.init") @Stepwise
class ToolEnvInitSpec extends FcliBaseSpec {
    @Shared @TempDir("fortify/tools") String baseDir
    @Shared @TempDir("fortify/path-test") String pathTestDir
    @Shared String scClientVersion = "23.1.0"
    @Shared String debrickedVersion = "2.6.7"
    @Shared String fcliVersion = "latest"
    // Platform-aware success marker - matches what fcli outputs
    @Shared String successMarker = System.getProperty("os.name", "").toLowerCase().contains("win") ? "[OK]" : "✓"
    
    // Parameterized tests for single tool setup (options-based)
    
    @Unroll
    def "init.options.#tool.#versionType"() {
        def args = "tool env init --tools=${toolSpec} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up ${tool}") }
                it.any { line -> line.contains(successMarker) && line.contains(tool) && (line.contains("installed") || line.contains("registered")) }
            }
        where:
            tool            | versionType | toolSpec
            "sc-client"     | "specific"  | "sc-client:23.1.0"
            "sc-client"     | "auto"      | "sc-client:auto"
            "sc-client"     | "latest"    | "sc-client:latest"
            "debricked-cli" | "specific"  | "debricked-cli:2.6.7"
            "debricked-cli" | "auto"      | "debricked-cli:auto"
            "debricked-cli" | "latest"    | "debricked-cli:latest"
            "fcli"          | "auto"      | "fcli:auto"
            "fcli"          | "latest"    | "fcli:latest"
    }
    
    // Parameterized tests for single tool setup (environment variable-based)
    
    @Unroll
    def "init.env.#tool.#versionType"() {
        def args = "tool env init --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up ${tool}") }
                it.any { line -> line.contains(successMarker) && line.contains(tool) && (line.contains("installed") || line.contains("registered")) }
            }
        where:
            tool            | versionType | env
            "sc-client"     | "specific"  | ["SC_CLIENT_VERSION": "23.1.0"]
            "sc-client"     | "auto"      | ["SC_CLIENT_VERSION": "auto"]
            "sc-client"     | "latest"    | ["SC_CLIENT_VERSION": "latest"]
            "debricked-cli" | "specific"  | ["DEBRICKED_VERSION": "2.6.7"]
            "debricked-cli" | "auto"      | ["DEBRICKED_VERSION": "auto"]
            "debricked-cli" | "latest"    | ["DEBRICKED_VERSION": "latest"]
            "fcli"          | "auto"      | ["FCLI_VERSION": "auto"]
            "fcli"          | "latest"    | ["FCLI_VERSION": "latest"]
    }
    
    // Tests for multiple tools (common CI integration scenario)
    
    def "init.multiple-tools.options"() {
        def args = "tool env init --tools=sc-client:${scClientVersion},fcli:${fcliVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && (line.contains("installed") || line.contains("registered")) }
                it.any { line -> line.contains(successMarker) && line.contains("fcli") && (line.contains("installed") || line.contains("registered")) }
            }
    }
    
    def "init.multiple-tools.env"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = [
            "SC_CLIENT_VERSION": scClientVersion,
            "FCLI_VERSION": fcliVersion
        ]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && (line.contains("installed") || line.contains("registered")) }
                it.any { line -> line.contains(successMarker) && line.contains("fcli") && (line.contains("installed") || line.contains("registered")) }
            }
    }
    
    def "init.multiple-tools.mixed-env-and-options"() {
        // When --tools is specified, it takes precedence; environment variables are ignored
        def args = "tool env init --tools=sc-client:${scClientVersion} --base-dir ${baseDir}"
        def env = [
            "FCLI_VERSION": fcliVersion,
            "DEBRICKED_VERSION": debrickedVersion
        ]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && (line.contains("installed") || line.contains("registered")) }
                // Environment variables should be ignored when --tools is specified
                !it.any { line -> line.contains("Setting up fcli") }
                !it.any { line -> line.contains("Setting up debricked-cli") }
            }
    }
    
    // Tests for preinstalled mode
    
    def "init.preinstalled-mode.options"() {
        // First ensure tool is installed
        def installArgs = "tool sc-client install -y -v=${scClientVersion} --base-dir ${baseDir}"
        Fcli.run(installArgs, {it.expectZeroExitCode()})
        
        // Then test preinstalled mode using explicit path
        def scClientPath = Path.of(baseDir).resolve("sc-client/${scClientVersion}")
        def args = "tool env init --tools=sc-client:${scClientPath} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && line.contains("registered") }
            }
    }
    
    def "init.preinstalled-mode.env"() {
        // Ensure tool is installed
        def installArgs = "tool sc-client install -y -v=${scClientVersion} --base-dir ${baseDir}"
        Fcli.run(installArgs, {it.expectZeroExitCode()})
        
        // Test using SC_CLIENT_HOME environment variable
        def scClientPath = Path.of(baseDir).resolve("sc-client/${scClientVersion}")
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["SC_CLIENT_HOME": scClientPath.toString()]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && line.contains("registered") }
            }
    }
    
    // Test --self option (used by fortify-setup-js to track fcli itself)
    
    def "init.self-option"() {
        def selfPath = "/usr/local/bin/fcli"
        def args = "tool env init --self=${selfPath} --tools=sc-client:${scClientVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && (line.contains("installed") || line.contains("registered")) }
            }
    }
    
    // Test error scenarios
    
    def "init.error.invalid-version"() {
        def args = "tool env init --tools=sc-client:99.99.99 --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectSuccess(false)})
        then:
            result.nonZeroExitCode
    }
    
    def "init.error.invalid-path"() {
        def args = "tool env init --tools=sc-client:/nonexistent/path --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectSuccess(false)})
        then:
            result.nonZeroExitCode
            result.stderr.any { line -> line.contains("not found") }
    }
    
    def "init.error.preinstalled-mode-not-found"() {
        def args = "tool env init --tools=sc-client:99.99.99 --base-dir ${baseDir} --preinstalled"
        when:
            def result = Fcli.run(args, {it.expectSuccess(false)})
        then:
            result.nonZeroExitCode
            result.stderr.any { line -> line.contains("not found") || line.contains("preinstalled mode") }
    }
    
    // Test install vs register behavior
    
    def "init.install-vs-register.first-install"() {
        // Clean install directory to ensure fresh start
        def scClientDir = Path.of(baseDir).resolve("sc-client/${scClientVersion}")
        if (Files.exists(scClientDir)) {
            scClientDir.toFile().deleteDir()
        }
        
        def args = "tool env init --tools=sc-client:${scClientVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && line.contains("installed") }
            }
            Files.exists(scClientDir)
    }
    
    def "init.install-vs-register.second-register"() {
        // Same version should register existing installation, not reinstall
        def args = "tool env init --tools=sc-client:${scClientVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && line.contains("registered") }
                // Should NOT contain "installed" since it's already present
                !it.any { line -> line.contains("installed") }
            }
    }
    
    def "init.install-vs-register.different-version-install"() {
        // Different version should install fresh
        def altVersion = "23.2.1"
        def altDir = Path.of(baseDir).resolve("sc-client/${altVersion}")
        if (Files.exists(altDir)) {
            altDir.toFile().deleteDir()
        }
        
        def args = "tool env init --tools=sc-client:${altVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains(successMarker) && line.contains("sc-client") && line.contains("installed") }
            }
            Files.exists(altDir)
    }
    
    // Test PATH-based lookup with mock executable
    
    def "init.path-lookup.create-mock-executable"() {
        // Create a mock debricked executable in pathTestDir for PATH-based testing
        def mockDebrickedDir = Path.of(pathTestDir).resolve("mock-debricked")
        Files.createDirectories(mockDebrickedDir)
        
        def mockExecutable = mockDebrickedDir.resolve("debricked")
        // Create a minimal shell script that responds to --version
        def scriptContent = '''#!/bin/sh
if [ "$1" = "--version" ]; then
    echo "2.6.0"
else
    echo "Mock debricked CLI"
fi
'''
        Files.writeString(mockExecutable, scriptContent)
        
        // Make executable (Unix-like systems)
        try {
            def perms = [
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
            ] as Set
            Files.setPosixFilePermissions(mockExecutable, perms)
        } catch (UnsupportedOperationException e) {
            // On Windows, permissions work differently; file is executable by default
            println("Skipping POSIX permissions on non-POSIX system")
        }
        
        expect:
            Files.exists(mockExecutable)
    }
    
    def "init.path-lookup.auto-detect-from-path"() {
        // Set PATH to include our mock executable directory
        // Note: If debricked-cli is already installed in baseDir from previous tests,
        // it will register that installation rather than looking in PATH.
        // This is expected behavior - existing installations take precedence.
        def mockDebrickedDir = Path.of(pathTestDir).resolve("mock-debricked")
        def originalPath = System.getenv("PATH") ?: ""
        def newPath = mockDebrickedDir.toString() + File.pathSeparator + originalPath
        
        def args = "tool env init --tools=debricked-cli:auto --base-dir ${baseDir}"
        def env = ["PATH": newPath]
        
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains(successMarker) && line.contains("debricked-cli") && line.contains("registered") }
                // Should register either from baseDir (if previously installed) or from PATH
            }
    }
    
    def "init.path-lookup.env-var-detection"() {
        // Test auto-detection from environment variable with PATH lookup
        def mockDebrickedDir = Path.of(pathTestDir).resolve("mock-debricked")
        def originalPath = System.getenv("PATH") ?: ""
        def newPath = mockDebrickedDir.toString() + File.pathSeparator + originalPath
        
        // No --tools specified, should auto-detect from DEBRICKED_VERSION env var
        def args = "tool env init --base-dir ${baseDir}"
        def env = [
            "DEBRICKED_VERSION": "auto",
            "PATH": newPath
        ]
        
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains(successMarker) && line.contains("debricked-cli") }
            }
    }
}
