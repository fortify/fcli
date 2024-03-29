package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserSupplier
import com.fortify.cli.ftest.fod._common.FoDUserGroupSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.rest") @FcliSession(FOD) @Stepwise
class FoDRestSpec extends FcliBaseSpec {
    def "list"() {
        def args = "fod rest call /api/v3/tenants"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=4
                it[1].contains("tenantName:")
            }
    }
    
}

