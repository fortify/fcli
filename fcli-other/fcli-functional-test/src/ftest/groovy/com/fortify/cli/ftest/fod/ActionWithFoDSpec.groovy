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
package com.fortify.cli.ftest.fod

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDReleaseSupplier

import spock.lang.Shared

@Prefix("fod.action.with-product") @FcliSession(FOD)
class ActionWithFoDSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/with-product-fod.yaml") String actionPath
    @Global(FoDReleaseSupplier.EightBall.class) FoDReleaseSupplier fodReleaseSupplier

    def "fod: REST helper accessible"() {
        when:
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode rest")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("FOD-REST-OK") }
            }
    }

    def "fod: SpEL function #fod.release accessible"() {
        when:
            def relId = fodReleaseSupplier.release.get("releaseId")
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode spel --id ${relId}")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("FOD-SPEL-OK") }
            }
    }
}
