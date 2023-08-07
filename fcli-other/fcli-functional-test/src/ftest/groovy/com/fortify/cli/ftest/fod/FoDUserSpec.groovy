package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDApp
import com.fortify.cli.ftest.fod._common.FoDUser
import com.fortify.cli.ftest.fod._common.FoDUserGroup
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.user") @FcliSession(FOD) @Stepwise
class FoDUserSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDUser user = new FoDUser().create()
    @Shared @AutoCleanup FoDUserGroup group = new FoDUserGroup().create()
    @Shared @AutoCleanup FoDApp app = new FoDApp().createWebApp()
    
    def "list"() {
        def args = "fod user list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("IdUsernameFirstNameLastNameEmailRole")
            }
    }
    
    def "get.byName"() {
        def args = "fod user get " + user.userName + " --store user"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    def "get.byId"() {
        def args = "fod user get ::user::userId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    
    def "update"() {
        def args = "fod user update fcliAutomatedTestUser --lastname updatedLastname --firstname updatedFirstname --phone 5678"
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
    
    def "updateAddGroups"() {
        def args = "fod user update " + user.userName + " --add-groups=" + group.groupName + " --firstname updatedFirstname2"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") && it.contains("updatedFirstname2") }
            }
    }
    
    def "verifyAddGroups"() {
        def args = "fod user-group get " + group.groupName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 1")
            }
    }
    
    def "updateAddApps"() {
        def args = "fod user update " + user.userName + " --add-apps=" + app.appName + " --email test2@test.test"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") && it.contains("test2@test.test") }
            }
    }
    
    def "verifyAddApps"() {
        //seems to take about 5 seconds to register, adding a few just in case
        Thread.sleep(10000)
        def userId= Fcli.run("util var contents user -o expr={userId}");
        def args = "fod rest call /api/v3/user-application-access/" + userId.stdout[0]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[2].contains(app.appName)
            }
    }
    
    def "updateRemoveGroups"() {
        def args = "fod user update " + user.userName + " --remove-groups=" + group.groupName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") && it.contains("UPDATED") }
            }
    }
    
    def "verifyRemoveGroups"() {
        def args = "fod user-group get " + group.groupName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 0")
            }
    }
    
    def "updateRemoveApps"() {
        def args = "fod user update " + user.userName + " --remove-apps=" + app.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("fcliAutomatedTestUser") && it.contains("UPDATED") }
            }
    }
    
    def "verifyRemovedApps"() {
        //seems to take about 5 seconds to register, adding a few just in case
        Thread.sleep(10000)
        def userId= Fcli.run("util var contents user -o expr={userId}");
        def args = "fod rest call /api/v3/user-application-access/" + userId.stdout[0]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[0].equals("--- []")
            }
    }
    
}

