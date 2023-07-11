package com.fortify.cli.functest.module.sc_dast;

import static com.fortify.cli.functest.common.spec.FcliSessionType.SCDAST

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession
import com.fortify.cli.functest.common.spec.Prefix

@Prefix("sc-dast.sensor") @FcliSession(SCDAST)
class SCDastSensorSpec extends BaseFcliSpec {
    def "list"() {
        expect:
            fcli "sc-dast", "sensor", "list"
    }
}