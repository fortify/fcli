package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.spec.BaseFcliSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

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