package com.fortify.cli.functest.module.sc_sast;

import static com.fortify.cli.functest.common.spec.FcliSessionType.SCSAST

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession

@FcliSession(SCSAST)
class SCSastSensorSpec extends BaseFcliSpec {
    def "sc-sast.scan.help"() {
        expect:
            fcli "sc-sast", "scan", "-h"
    }
}