package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared

@Prefix("core.action.exitcode")
class ActionExitCodeSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/exit-code.yaml") String exitCodeActionPath

    @spock.lang.Unroll
    def "exit-code matrix: exitCode=#exitCode failCheck=#failCheck runFailCmd=#runFailCmd => expected #expectedExit"() {
        when:
            def actionPath = exitCodeActionPath
            def args = "action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore"
            if ( exitCode!=null ) { args += " --exit-code=${exitCode}" }
            if ( failCheck ) { args += " --fail-check=true" }
            if ( runFailCmd ) { args += " --run-failing-cmd=true" }
            // Do not validate success/failed here; test asserts expected exit code explicitly
            def result = Fcli.run(args, { res -> /* no-op: let test assert exit code */ })
        then:
           assert result.exitCode == expectedExit

        where:
            exitCode | failCheck | runFailCmd || expectedExit
            42       | false     | false      || 42
            0        | false     | false      || 0
            0        | true      | false      || 100
            1        | true      | false      || 101
            null     | false     | true       || 1
             0       | false     | true       || 1
             0       | true      | true       || 1
             1       | true      | true       || 1
    }
}
