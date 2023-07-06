package com.fortify.cli.functest.module.sc_dast;

import static com.fortify.cli.functest.common.spec.FcliSessionType.SCDAST

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession

@FcliSession(SCDAST)
class SCDastSensorSpec extends BaseFcliSpec {
    def "sc-dast.sensor.list"() {
        expect:
            fcli "sc-dast", "sensor", "list"
    }
}