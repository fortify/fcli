package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix

import spock.lang.Unroll

@Prefix("fod.app") @FcliSession(FOD)
class FoDAppSpec extends FcliBaseSpec {
    @Unroll
    def "list"() {
        def args = "fod app list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
        where:
            i << (1..5)
    }
}