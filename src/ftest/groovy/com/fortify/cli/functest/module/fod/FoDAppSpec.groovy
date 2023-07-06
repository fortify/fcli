package com.fortify.cli.functest.module.fod;

import static com.fortify.cli.functest.common.spec.FcliSessionType.FOD

import com.fortify.cli.functest.common.spec.BaseFcliSpec
import com.fortify.cli.functest.common.spec.FcliSession
import com.fortify.cli.functest.common.spec.Prefix

import spock.lang.Unroll

@Prefix("fod.app") @FcliSession(FOD)
class FoDAppSpec extends BaseFcliSpec {
    @Unroll
    def "list"() {
        expect:
            fcli "fod", "app", "list"
        where:
            i << (1..5)
    }
}