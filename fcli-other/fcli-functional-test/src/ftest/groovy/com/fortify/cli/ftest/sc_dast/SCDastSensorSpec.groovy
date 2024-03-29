package com.fortify.cli.ftest.sc_dast;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SCDAST

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-dast.sensor") @FcliSession(SCDAST)
class SCDastSensorSpec extends FcliBaseSpec {
    def "list"() {
        def args = "sc-dast sensor list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
}