package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FODAppRel
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.user") @FcliSession(FOD) @Stepwise
class FoDUserSpec extends FcliBaseSpec {
    
    def "list"() {
        def args = "fod user list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=1
                if(size()>1) {
                    it[0].replace(' ', '').equals("IdUsernameFirstNameLastNameEmailRole")
                } else {
                    it[0].equals("No data")
                }
            }
    }
    
    def "getRoles"() {
        def args = "fod rest lookup --type Roles --store roles"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>1
                it[0].replace(' ', '').equals("GroupTextValue")
            }
    }
    
    def "create"() {
        def args = "fod user create fcliAutomatedTestUser --email=test@test.test --firstname=test --lastname=user --phone=1234 --role=::roles::get(0).value"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[0].replace(' ', '').equals("IdUsernameFirstNameLastNameEmailRoleAction")
            }
    }
    
    def "verifyCreated"() {
        def args = "fod user list --store users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") }
            }
    }
    
    
    def "get.byId"() {
        def args = "fod user get ::users::get(0).userId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    def "get.byName"() {
        def args = "fod user get ::users::get(0).userName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    
    def "update"() {
        def args = "fod user update fcliAutomatedTestUser --lastname updatedLastname"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod user list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") && it.contains("updatedLastname") }
            }
    }
    
    def "delete"() {
        def args = "fod user delete fcliAutomatedTestUser"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyDeleted"() {
        def args = "fod user list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains("fcliAutomatedTestUser") }
            }
    }
    
}

