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
package com.fortify.cli.ftest.sc_dast

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCDAST
import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCSAST

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared

@Prefix("sc-sast.rest.call") @FcliSession(SCDAST)
class SCDastRestCallSpec extends FcliBaseSpec {
    def "user-permissions"() {
        def args = ["sc-dast", "rest", "call", "/api/v2/auth/user-permissions"]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0] == '---'
                it[1].startsWith '-'
                it.any { it =~ 'username' }
                it.any { it =~ 'permissions' }
            }
    }
    
    def "transform-no-paging"() {
        def args = ["sc-dast", "rest", "call", "-X", "GET", "/api/v2/applications?limit=1", "--no-paging", '-t', 'items.![{x: id}]']
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0] == '---'
                it[1].startsWith '- x:'
            }
    }
}
