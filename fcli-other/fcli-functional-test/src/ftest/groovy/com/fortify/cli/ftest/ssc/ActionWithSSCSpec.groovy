/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.ssc

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.Shared

@Prefix("ssc.action.with-product") @FcliSession(SSC)
class ActionWithSSCSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/with-product-ssc.yaml") String actionPath
    @Global(SSCAppVersionSupplier.EightBall.class) SSCAppVersionSupplier sscVersionSupplier

    def "ssc: REST helper accessible"() {
        when:
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode rest")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("SSC-REST-OK") }
            }
    }

    def "ssc: SpEL function #ssc.appVersion accessible"() {
        when:
            def avId = sscVersionSupplier.version.get("id")
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode spel --id ${avId}")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("SSC-SPEL-OK") }
            }
    }
}
