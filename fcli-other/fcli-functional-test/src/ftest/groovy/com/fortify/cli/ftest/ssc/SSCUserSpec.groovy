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
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.user") @FcliSession(SSC) @Stepwise
class SSCUserSpec extends FcliBaseSpec {
    
    def "list"() {
        def args = "ssc user list --store users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdEntitynameDisplaynameTypeEmailIsldap")
                it.any { it.startsWith(" 1") }
            }
    }
    
    def "get.byId"() {
        def args = "ssc user get ::users::get(0).id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].startsWith("isLdap: ")
            }
    }
    
    def "get.byName"() {
        def args = "ssc user get ::users::get(0).entityName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].startsWith("isLdap: ")
            }
    }
    
    def "get.byMail"() {
        def args = "ssc user get ::users::get(0).email"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].startsWith("isLdap: ")
            }
    }
    
    //TODO add tests for delete? what about create?
}
