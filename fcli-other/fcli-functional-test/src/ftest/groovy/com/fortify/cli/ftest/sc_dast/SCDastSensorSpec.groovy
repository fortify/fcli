package com.fortify.cli.ftest.sc_dast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCDAST

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-dast.sensor") @FcliSession(SCDAST)
class SCDastSensorSpec extends FcliBaseSpec {
    def "list"() {
        expect:
            verifyAll(Fcli.run("sc-dast", "sensor", "list")) {
                // TODO Add expectations
            }
    }
}