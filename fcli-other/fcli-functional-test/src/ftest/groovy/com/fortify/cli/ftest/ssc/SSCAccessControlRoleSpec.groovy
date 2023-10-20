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
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier.SSCRole
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.role") @FcliSession(SSC) @Stepwise
class SSCAccessControlRoleSpec extends FcliBaseSpec {
    @Shared SSCRoleSupplier roleSupplier = new SSCRoleSupplier();
    
    def "list"() {
        def args = "ssc ac list-roles"
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
            SSCRole role = roleSupplier.role;
        then:
            noExceptionThrown()
    }
    
    def "get.byName"() {
        def args = "ssc ac get-role " + roleSupplier.role.roleName + " --store role"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + roleSupplier.role.roleName + "\"")
            }
    }
    
    def "get.byId"() {
        def args = "ssc ac get-role ::role::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals("name: \"" + roleSupplier.role.roleName + "\"")
            }
    }
    
    def "delete"() {
        when:
            roleSupplier.role.close();
        then:
            noExceptionThrown()
    }
    
    def "verifyDeleted"() {
        def args = "ssc ac list-roles"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                !it.any { it.contains(roleSupplier.role.roleName) }
            }
    }
}
