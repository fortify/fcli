package com.fortify.cli.ftest.core

import java.nio.file.Files
import java.nio.file.Path

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("core.ai-assist.extensions") @Stepwise
class AiAssistExtensionsSpec extends FcliBaseSpec {
    @Shared @TempDir("ai-assist/extensions") String baseDir;

    def "list-versions"() {
        when:
            def result = Fcli.run("ai-assist extensions list-versions")
        then:
            verifyAll(result.stdout) {
                size() > 1
                it[0].replace(' ', '').equals("VersionAliasesStable")
            }
    }

    def "list-assistants"() {
        when:
            def result = Fcli.run("ai-assist extensions list-assistants")
        then:
            verifyAll(result.stdout) {
                size() > 1
                it[0].replace(' ', '').equals("IdNameContenttypesDetectedInstalled")
            }
    }

    def "list-assistants-detect"() {
        when:
            def result = Fcli.run("ai-assist extensions list-assistants --detect")
        then:
            verifyAll(result.stdout) {
                size() > 1
                it[0].replace(' ', '').equals("IdNameContenttypesDetectedInstalled")
                // With --detect, Detected column should show true/false, not N/A
                !it[1].contains("N/A")
            }
    }

    def "setup-no-target"() {
        when:
            def result = Fcli.run("ai-assist extensions setup --dry-run",
                {it.expectSuccess(false)})
        then:
            verifyAll(result.stderr) {
                it.any { it.contains("--assistants") || it.contains("--auto-detect") || it.contains("--dir") }
            }
    }

    def "setup-unknown-assistant"() {
        when:
            def result = Fcli.run("ai-assist extensions setup --assistants unknown-assistant --dry-run",
                {it.expectSuccess(false)})
        then:
            verifyAll(result.stderr) {
                it.any { it.contains("Unknown assistant: unknown-assistant") }
            }
    }

    def "setup-dry-run"() {
        def targetDir = "${baseDir}/skills-dry-run"
        when:
            def result = Fcli.run("ai-assist extensions setup --dir ${targetDir} --content-types skills --dry-run",
                {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it[0].replace(' ', '').equals("AssistantContenttypeTargetdirFilecountAction")
                it[1].contains("INSTALLED")
            }
            // Dry run should not create files
            !Files.exists(Path.of(targetDir))
    }

    def "setup-install"() {
        def targetDir = "${baseDir}/skills"
        when:
            def result = Fcli.run("ai-assist extensions setup --dir ${targetDir} --content-types skills",
                {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it[0].replace(' ', '').equals("AssistantContenttypeTargetdirFilecountAction")
                it[1].contains("INSTALLED")
            }
            // Files should exist now
            Files.exists(Path.of(targetDir))
            Files.exists(Path.of(targetDir, ".fortify-extensions.skills.json"))
    }

    def "list-installed-after-setup"() {
        when:
            def result = Fcli.run("ai-assist extensions list-installed")
        then:
            verifyAll(result.stdout) {
                // --dir mode doesn't record installations state, so still "No data"
                size() == 1
                it[0].trim() == "No data"
            }
    }

    def "setup-idempotent"() {
        def targetDir = "${baseDir}/skills"
        when:
            def result = Fcli.run("ai-assist extensions setup --dir ${targetDir} --content-types skills",
                {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it[0].replace(' ', '').equals("AssistantContenttypeTargetdirFilecountAction")
                // Re-running setup on same dir should report UNCHANGED
                it[1].contains("UNCHANGED")
            }
    }

    def "uninstall-dir"() {
        def targetDir = "${baseDir}/skills"
        when:
            def result = Fcli.run("ai-assist extensions uninstall --dir ${targetDir} --content-types skills",
                {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size() > 0
                it[0].replace(' ', '').equals("ContenttypeTargetdirFilecountAction")
                it[1].contains("REMOVED")
            }
            // Manifest should be gone
            !Files.exists(Path.of(targetDir, ".fortify-extensions.skills.json"))
    }
}
