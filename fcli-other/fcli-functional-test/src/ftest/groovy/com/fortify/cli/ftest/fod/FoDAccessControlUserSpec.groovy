package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserSupplier
import com.fortify.cli.ftest.fod._common.FoDUserGroupSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.user") @FcliSession(FOD) @Stepwise
class FoDAccessControlUserSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDUserSupplier user = new FoDUserSupplier()
    @Shared @AutoCleanup FoDUserGroupSupplier group = new FoDUserGroupSupplier()
    @Shared @AutoCleanup FoDWebAppSupplier app = new FoDWebAppSupplier()
    
    def "list"() {
        def args = "fod ac list-users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("IdUsernameFirstNameLastNameEmailRole")
            }
    }
    
    def "get.byName"() {
        def args = "fod ac get-user ${user.get().userName} --store user"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    def "get.byId"() {
        def args = "fod ac get-user ::user::userId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }
    
    
    def "update"() {
        def args = "fod ac update-user ${user.get().userName} --lastname updatedLastname --firstname updatedFirstname --phone 5678"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod ac list-users"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("${user.get().userName}") && it.contains("updatedLastname") }
            }
    }
    
    def "updateAddGroups"() {
        def args = "fod ac update-user ${user.get().userName} --add-groups=${group.get().groupName} --firstname updatedFirstname2"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("${user.get().userName}") && it.contains("updatedFirstname2") }
            }
    }
    
    def "verifyAddGroups"() {
        def args = "fod ac get-group ${group.get().groupName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 1")
            }
    }
    
    def "updateAddApps"() {
        def args = "fod ac update-user ${user.get().userName} --add-apps=${app.get().appName} --email test2@test.test"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("${user.get().userName}") && it.contains("test2@test.test") }
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
                it[2].contains(app.get().appName)
            }
    }
    
    def "updateRemoveGroups"() {
        def args = "fod ac update-user ${user.get().userName} --remove-groups=${group.get().groupName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("${user.get().userName}") && it.contains("UPDATED") }
            }
    }
    
    def "verifyRemoveGroups"() {
        def args = "fod ac get-group ${group.get().groupName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 0")
            }
    }
    
    def "updateRemoveApps"() {
        def args = "fod ac update-user ${user.get().userName} --remove-apps=${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("${user.get().userName}") && it.contains("UPDATED") }
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

