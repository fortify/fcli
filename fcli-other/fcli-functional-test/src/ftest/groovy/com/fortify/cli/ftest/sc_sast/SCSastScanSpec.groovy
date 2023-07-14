package com.fortify.cli.ftest.sc_sast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCSAST

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-sast.scan") @FcliSession(SCSAST)
class SCSastSensorSpec extends FcliBaseSpec {
    def "help"() {
        expect:
            verifyAll(Fcli.run("sc-sast", "scan", "-h")) {
                // TODO Add expectations
            }
    }
}