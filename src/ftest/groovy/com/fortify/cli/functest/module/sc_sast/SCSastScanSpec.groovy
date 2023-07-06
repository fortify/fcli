package com.fortify.cli.functest.module.sc_sast;

import static com.fortify.cli.functest.common.spec.FcliSessionType.SCSAST

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession
import com.fortify.cli.functest.common.spec.Prefix

@Prefix("sc-sast.scan") @FcliSession(SCSAST)
class SCSastSensorSpec extends BaseFcliSpec {
    def "help"() {
        expect:
            fcli "sc-sast", "scan", "-h"
    }
}