package com.fortify.cli.ftest.sc_sast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCSAST

import com.fortify.cli.ftest._common.spec.BaseFcliSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("sc-sast.scan") @FcliSession(SCSAST)
class SCSastSensorSpec extends BaseFcliSpec {
    def "help"() {
        expect:
            fcli "sc-sast", "scan", "-h"
    }
}