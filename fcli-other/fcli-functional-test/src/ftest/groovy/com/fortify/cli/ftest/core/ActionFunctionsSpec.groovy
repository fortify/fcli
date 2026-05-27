package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.action.functions")
class ActionFunctionsSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/functions.yaml") String functionsActionPath
    @Shared @TestResource("runtime/actions/run-fcli-shared-state-parent.yaml") String runFcliSharedStateParentActionPath
    @Shared @TestResource("runtime/actions/run-fcli-shared-state-child.yaml") String runFcliSharedStateChildActionPath
    
    def "fn-call-spel"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-spel")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("Hello, World!") }
            }
    }
    
    def "fn-call-streaming-spel"() {
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
    def "fn-call-composed"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-composed")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("Hello, Test!") && it.contains("Sum=30") }
            }
    }

    def "fn-call-internal"() {
        when:
            def result = Fcli.run("action run ${functionsActionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fn-call-internal")
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("internal-value") }
            }
    }

    def "run.fcli reuses parent action state"() {
        when:
            def result = Fcli.run([
                "action", "run", runFcliSharedStateParentActionPath,
                "--progress=none",
                "--on-unsigned=ignore",
                "--on-invalid-version=ignore",
                "--child-action-path", runFcliSharedStateChildActionPath
            ])
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("before=red") }
                it.any { it.contains("after=blue") }
            }
    }
}
