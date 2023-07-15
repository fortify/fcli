package com.fortify.cli.ftest.sc_sast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCSAST

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-sast.scan") @FcliSession(SCSAST)
class SCSastSensorSpec extends FcliBaseSpec {
    def "help"() {
        def args = ["sc-sast", "scan", "-h"]
        when:
            def result = Fcli.runOrFail(args)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
}