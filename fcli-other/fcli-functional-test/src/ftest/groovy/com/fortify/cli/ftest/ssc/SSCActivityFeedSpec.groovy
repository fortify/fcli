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

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

@Prefix("ssc.activity-feed") @FcliSession(SSC)
@Requires({System.getProperty('ft.include.long-running')})
class SSCActivityFeedSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier = new SSCAppVersionSupplier()
    
    def "list"() {
        def args = "ssc activity-feed list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) { 
                size()>0
                it.any { it =~ "PROJECT_VERSION_CREATED" }
                it.any { it =~ "Application: "+versionSupplier.version.appName }
                it.any { it =~ "Version: "+versionSupplier.version.versionName }
            }
    }
}
