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

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise

/**
 * Functional tests for 'fcli tool env init' command.
 * 
 * Tests cover both option-based and environment variable-based configuration
 * for the most common CI integration scenarios: sc-client, debricked-cli, and fcli.
 */
@Prefix("tool.env.init") @Stepwise
class ToolEnvInitSpec extends FcliBaseSpec {
    @Shared @TempDir("fortify/tools") String baseDir;
    @Shared String scClientVersion = "23.1.0"
    @Shared String debrickedVersion = "2.6.7"
    @Shared String fcliVersion = "latest"
    
    // Tests for sc-client
    
    def "init.sc-client.options.specific-version"() {
        def args = "tool env init --tools=sc-client:${scClientVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    def "init.sc-client.env.specific-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["SC_CLIENT_VERSION": scClientVersion]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    def "init.sc-client.options.auto-version"() {
        def args = "tool env init --tools=sc-client:auto --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    def "init.sc-client.env.auto-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["SC_CLIENT_VERSION": "auto"]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    def "init.sc-client.options.latest-version"() {
        def args = "tool env init --tools=sc-client:latest --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    def "init.sc-client.env.latest-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["SC_CLIENT_VERSION": "latest"]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up sc-client") }
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
            }
    }
    
    // Tests for debricked-cli
    
    def "init.debricked-cli.options.specific-version"() {
        def args = "tool env init --tools=debricked-cli:${debrickedVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains("✓") && line.contains("debricked-cli") }
            }
    }
    
    def "init.debricked-cli.env.specific-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["DEBRICKED_VERSION": debrickedVersion]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains("✓") && line.contains("debricked-cli") }
            }
    }
    
    def "init.debricked-cli.options.auto-version"() {
        def args = "tool env init --tools=debricked-cli:auto --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains("✓") && line.contains("debricked-cli") }
            }
    }
    
    def "init.debricked-cli.env.auto-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["DEBRICKED_VERSION": "auto"]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up debricked-cli") }
                it.any { line -> line.contains("✓") && line.contains("debricked-cli") }
            }
    }
    
    // Tests for fcli
    
    def "init.fcli.options.latest-version"() {
        def args = "tool env init --tools=fcli:${fcliVersion} --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
            }
    }
    
    def "init.fcli.env.latest-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["FCLI_VERSION": fcliVersion]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
            }
    }
    
    def "init.fcli.options.auto-version"() {
        def args = "tool env init --tools=fcli:auto --base-dir ${baseDir}"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
            }
    }
    
    def "init.fcli.env.auto-version"() {
        def args = "tool env init --base-dir ${baseDir}"
        def env = ["FCLI_VERSION": "auto"]
        when:
            def result = Fcli.run(args, env, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it.any { line -> line.contains("Setting up fcli") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
            }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
                it.any { line -> line.contains("✓") && line.contains("fcli") }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") && line.contains("registered") }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") && line.contains("registered") }
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
                it.any { line -> line.contains("✓") && line.contains("sc-client") }
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
}
