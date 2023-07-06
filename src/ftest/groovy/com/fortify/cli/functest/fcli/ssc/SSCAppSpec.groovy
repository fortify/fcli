package com.fortify.cli.functest.fcli.ssc;

import static com.fortify.cli.functest.common.spec.FcliSessionType.SSC

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession

import spock.lang.Unroll

@FcliSession(SSC)
class SSCAppSpec extends BaseFcliSpec {
    @Unroll("fcli.ssc.list (#i)")
    def "ssc.app.list"() {
        expect:
        fcli "ssc", "appversion", "list"
        where:
        i << (1..20)
    }
}