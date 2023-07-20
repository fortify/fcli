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
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared

@Prefix("ssc.rest.call") @FcliSession(SSC)
class SSCRestCallSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    
    def "session-info"() {
        def args = ["ssc", "rest", "call", "-X", "POST", "/api/v1/userSession/info", "-d", "ignored"]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0] == '---'
                it[1].startsWith '- '
                it.any { it =~ 'username' }
                it.any { it =~ 'permissions' }
            }
    }
}
