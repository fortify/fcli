package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.spec.BaseFcliSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

import spock.lang.Unroll

@Prefix("ssc.app") @FcliSession(SSC)
class SSCAppSpec extends BaseFcliSpec {
    @Unroll
    def "list"() {
        expect:
        fcli "ssc", "app", "list"
        where:
        i << (1..20)
    }
}