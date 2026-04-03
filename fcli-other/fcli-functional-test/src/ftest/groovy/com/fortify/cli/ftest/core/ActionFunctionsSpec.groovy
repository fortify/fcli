package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.action.functions")
class ActionFunctionsSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/functions.yaml") String functionsActionPath

    def "fn.call step: non-streaming add function"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-step")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("7") }
            }
    }

    def "fn.call SpEL: #fn.call greet function"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-spel")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("Hello, World!") }
            }
    }

    def "fn.call step: streaming each function"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-streaming")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("1") }
                it.any { it.contains("2") }
                it.any { it.contains("3") }
            }
    }

    def "records.for-each with #fn.call: streaming is lazy (yield-process interleaved)"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-streaming-spel")
        then:
            // Verify yield/process pairs are interleaved, proving lazy streaming
            def lines = result.stdout.findAll { it.startsWith("Yield:") || it.startsWith("Process:") }
            lines.size() == 6
            lines[0] == "Yield: 10"
            lines[1] == "Process: 10"
            lines[2] == "Yield: 20"
            lines[3] == "Process: 20"
            lines[4] == "Yield: 30"
            lines[5] == "Process: 30"
    }

    def "fn.call step: composed function calling other functions"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-composed")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("Hello, Test!") && it.contains("Sum=30") }
            }
    }

    def "fn.call step: internal non-exported function is callable from steps"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-internal")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("internal-value") }
            }
    }
}
