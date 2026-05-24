package com.fortify.cli.ftest.core

import java.nio.file.Files
import java.nio.file.Path

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("core.ai-assist.mcp") @Stepwise
class AiAssistMcpSpec extends FcliBaseSpec {
    @Shared @TempDir("ai-assist/mcp") String baseDir;

    def "create-http-config-ssc"() {
        def configPath = "${baseDir}/mcp-http-config-ssc.yaml"
        when:
            def result = Fcli.run("ai-assist mcp create-http-config --type ssc --config ${configPath}",
                {it.expectZeroExitCode()})
        then:
            verifyAll {
                Files.exists(Path.of(configPath))
                Files.readString(Path.of(configPath)).contains("ssc")
            }
    }

    def "create-http-config-fod"() {
        def configPath = "${baseDir}/mcp-http-config-fod.yaml"
        when:
            def result = Fcli.run("ai-assist mcp create-http-config --type fod --config ${configPath}",
                {it.expectZeroExitCode()})
        then:
            verifyAll {
                Files.exists(Path.of(configPath))
                Files.readString(Path.of(configPath)).contains("fod")
            }
    }

    def "create-http-config-no-overwrite"() {
        def configPath = "${baseDir}/mcp-http-config-ssc.yaml"
        when:
            def result = Fcli.run("ai-assist mcp create-http-config --type ssc --config ${configPath}",
                {it.expectSuccess(false)})
        then:
            verifyAll(result.stderr) {
                it.any { it.contains("already exists") }
            }
    }

    def "create-http-config-force-overwrite"() {
        def configPath = "${baseDir}/mcp-http-config-ssc.yaml"
        when:
            def result = Fcli.run("ai-assist mcp create-http-config --type ssc --config ${configPath} --force",
                {it.expectZeroExitCode()})
        then:
            verifyAll {
                Files.exists(Path.of(configPath))
                Files.readString(Path.of(configPath)).contains("ssc")
            }
    }
}
