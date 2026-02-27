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
import com.fortify.cli.ftest._common.spec.TempFile

import spock.lang.Shared
import spock.lang.Stepwise

/**
 * Functional tests for 'fcli tool env' output-emitting commands:
 * shell, pwsh, ado, github, gitlab.
 * 
 * Covers both the native (CI-system-native) output and the --output-as=shell|pwsh
 * variants that combine CI signals with direct env-var assignments.
 * 
 * Setup: 'fcli tool env init --tools=fcli:auto' is used to register the currently
 * running fcli so that all env commands have at least one tool installation to emit.
 */
@Prefix("tool.env.emit") @Stepwise
class ToolEnvEmitSpec extends FcliBaseSpec {
    @Shared @TempDir("tool.env.emit/base")         String baseDir
    @Shared @TempFile("tool.env.emit/shell-out")   String shellFile
    @Shared @TempFile("tool.env.emit/pwsh-out")    String pwshFile
    @Shared @TempFile("tool.env.emit/github-env")  String githubEnvFile
    @Shared @TempFile("tool.env.emit/github-path") String githubPathFile
    @Shared @TempFile("tool.env.emit/gitlab-env")  String gitlabFile

    def setupSpec() {
        Fcli.run("tool env init --tools=fcli:auto --base-dir ${baseDir}", {it.expectZeroExitCode()})
    }

    // -------------------------------------------------------------------------
    // tool env shell
    // -------------------------------------------------------------------------

    def "shell.stdout"() {
        when:
            def result = Fcli.run("tool env shell", {it.expectZeroExitCode()})
        then:
            result.stdout.any { it.startsWith("export PATH=") }
    }

    def "shell.file"() {
        when:
            Fcli.run("tool env shell --file ${shellFile}", {it.expectZeroExitCode()})
        then:
            new File(shellFile).text.contains("export PATH=")
    }

    // -------------------------------------------------------------------------
    // tool env pwsh
    // -------------------------------------------------------------------------

    def "pwsh.stdout"() {
        when:
            def result = Fcli.run("tool env pwsh", {it.expectZeroExitCode()})
        then:
            result.stdout.any { it.startsWith('$env:PATH') }
    }

    def "pwsh.file"() {
        when:
            Fcli.run("tool env pwsh --file ${pwshFile}", {it.expectZeroExitCode()})
        then:
            new File(pwshFile).text.contains('$env:PATH')
    }

    // -------------------------------------------------------------------------
    // tool env ado
    // -------------------------------------------------------------------------

    def "ado.native"() {
        when:
            def result = Fcli.run("tool env ado", {it.expectZeroExitCode()})
        then:
            result.stdout.any { it.startsWith("##vso[task.prependpath]") }
    }

    def "ado.output-as.shell"() {
        when:
            def result = Fcli.run("tool env ado --output-as=shell", {it.expectZeroExitCode()})
        then:
            result.stdout.any { it.startsWith("echo '##vso[task.prependpath]") }
            result.stdout.any { it.startsWith("export PATH=") }
    }

    def "ado.output-as.pwsh"() {
        when:
            def result = Fcli.run("tool env ado --output-as=pwsh", {it.expectZeroExitCode()})
        then:
            result.stdout.any { it.startsWith("echo '##vso[task.prependpath]") }
            result.stdout.any { it.startsWith('$env:PATH') }
    }

    // -------------------------------------------------------------------------
    // tool env github
    // -------------------------------------------------------------------------

    def "github.native"() {
        when:
            Fcli.run("tool env github",
                    ["GITHUB_ENV": githubEnvFile, "GITHUB_PATH": githubPathFile],
                    {it.expectZeroExitCode()})
        then:
            // GITHUB_PATH file gets one bin-dir line per tool
            Files.size(Path.of(githubPathFile)) > 0
    }

    def "github.output-as.shell"() {
        when:
            def result = Fcli.run("tool env github --output-as=shell", {it.expectZeroExitCode()})
        then:
            // PATH redirect echoed to $GITHUB_PATH
            result.stdout.any { it.matches("echo '.+' >> \\\$GITHUB_PATH") }
            // Direct shell export for the current task
            result.stdout.any { it.startsWith("export PATH=") }
    }

    def "github.output-as.pwsh"() {
        when:
            def result = Fcli.run("tool env github --output-as=pwsh", {it.expectZeroExitCode()})
        then:
            // PATH redirect echoed to $env:GITHUB_PATH
            result.stdout.any { it.matches("echo '.+' >> \\\$env:GITHUB_PATH") }
            // Direct PowerShell assignment for the current task
            result.stdout.any { it.startsWith('$env:PATH') }
    }

    // -------------------------------------------------------------------------
    // tool env gitlab
    // -------------------------------------------------------------------------

    def "gitlab.native"() {
        when:
            Fcli.run("tool env gitlab --file ${gitlabFile}", {it.expectZeroExitCode()})
        then:
            new File(gitlabFile).text.contains("PATH=")
    }

    def "gitlab.output-as.shell"() {
        when:
            def result = Fcli.run("tool env gitlab --file ${gitlabFile} --output-as=shell",
                    {it.expectZeroExitCode()})
        then:
            // The echo redirect target is the resolved absolute file path
            result.stdout.any { it.matches("echo '.+' >> .+") && it.contains(gitlabFile) }
            // Direct shell export for the current task
            result.stdout.any { it.startsWith("export PATH=") }
    }

    def "gitlab.output-as.pwsh"() {
        when:
            def result = Fcli.run("tool env gitlab --file ${gitlabFile} --output-as=pwsh",
                    {it.expectZeroExitCode()})
        then:
            // The echo redirect target is the resolved absolute file path
            result.stdout.any { it.matches("echo '.+' >> .+") && it.contains(gitlabFile) }
            // Direct PowerShell assignment for the current task
            result.stdout.any { it.startsWith('$env:PATH') }
    }

    def cleanupSpec() {
        cleanupAllTools()
    }
}
