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
import com.fortify.cli.ftest.ssc._common.SSCRole
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.role") @FcliSession(SSC) @Stepwise
class SSCRoleSpec extends FcliBaseSpec {
    @Shared SSCRole role = null;
    
    def "list"() {
        def args = "ssc role list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameBuiltinAllapplicationroleDescription")
                it.any { it.startsWith(" admin") }
            }
    }
    
    def "create"() {
        when:
            role = new SSCRole().create();
        then:
            noExceptionThrown()
    }
    
    def "get.byName"() {
        def args = "ssc role get " + role.roleName + " --store role"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + role.roleName + "\"")
            }
    }
    
    def "get.byId"() {
        def args = "ssc role get ::role::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + role.roleName + "\"")
            }
    }
    
    def "delete"() {
        when:
            role.close();
        then:
            noExceptionThrown()
    }
    
    def "verifyDeleted"() {
        def args = "ssc role list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                !it.any { it.contains(role.roleName) }
            }
    }
}
