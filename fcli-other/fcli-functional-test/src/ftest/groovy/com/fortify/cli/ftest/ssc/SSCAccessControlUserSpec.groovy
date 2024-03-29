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
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.user") @FcliSession(SSC) @Stepwise
class SSCAccessControlUserSpec extends FcliBaseSpec {
    @Shared private final String random = System.currentTimeMillis()
    @Shared private final String userName = "fclitemporarytestuser"+random
    
    @Shared private String unameval = "userName: \"$userName\""
    @Shared private String entityval = "entityName: \"$userName\""
    @Shared private String mailval = "email: \"$random@mail.mail\""
    
    def "list"() {
        def args = "ssc ac list-users --store users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdEntitynameDisplaynameTypeEmailIsldap")
                it.any { it.startsWith(" 1") }
            }
    }
    
    def "create"() {
        def args = "ssc ac create-local-user --username $userName --password P@ssW._ord123 --pne --suspend --rpc --firstname fName --lastname lName --email $random@mail.mail --roles viewonly --store user"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[2].equals(unameval)
                it[3].equals("firstName: \"fName\"")
                it[4].equals("lastName: \"lName\"")
                it[5].equals(mailval)
                !it[11].equals("roles: null")
            }
    }
    
    def "get.byId"() {
        def args = "ssc ac get-user ::user::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[3].equals(entityval)
            }
    }
    
    def "get.byName"() {
        def args = "ssc ac get-user ::user::userName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[3].equals(entityval)
            }
    }
    
    def "get.byMail"() {
        def args = "ssc ac get-user ::user::email"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[3].equals(entityval)
            }
    }
    
    def "delete"() {
        def args = "ssc ac delete-user ::user::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("DELETED")
            }
    }
    
    def "verifyDeleted"() {
        def args = "ssc ac list-users --store users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                !it.any { it.contains(userName) }
            }
    }
}
