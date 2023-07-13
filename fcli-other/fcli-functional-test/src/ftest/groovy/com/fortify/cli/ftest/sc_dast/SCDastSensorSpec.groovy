package com.fortify.cli.ftest.sc_dast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCDAST

import com.fortify.cli.ftest._common.spec.BaseFcliSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-dast.sensor") @FcliSession(SCDAST)
class SCDastSensorSpec extends BaseFcliSpec {
    def "list"() {
        expect:
            fcli "sc-dast", "sensor", "list"
    }
}