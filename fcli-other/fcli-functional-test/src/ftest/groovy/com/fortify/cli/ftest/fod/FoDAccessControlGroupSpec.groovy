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

@Prefix("fod.usergroup") @FcliSession(FOD) @Stepwise
class FoDAccessControlGroupSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDUserSupplier user = new FoDUserSupplier()
    @Shared @AutoCleanup FoDUserGroupSupplier group = new FoDUserGroupSupplier()
    @Shared @AutoCleanup FoDWebAppSupplier app = new FoDWebAppSupplier()
    
    def "list"() {
        def args = "fod ac list-groups"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("IdNameAssignedUsersAssignedApplications")
            }
    }
    
    def "get.byName"() {
        def args = "fod ac get-group ${group.get().groupName} --store group" 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=6
                it[2].contains(group.get().groupName)
            }
    }
    
    def "get.byId"() {
        def args = "fod ac get-group ::group::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=6
                it[2].contains(group.get().groupName)
            }
    }
    
    
    def "update"() {
        def args = "fod ac update-group ${group.get().groupName} --add-users ${user.get().userName} --add-apps ${app.get().appName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("CREATED")
            }
    }
    
    def "verifyUpdate"() {
        //app assignment seems to take about 5 seconds to register, adding a few just in case
        Thread.sleep(10000)
        def args = "fod ac get-group ${group.get().groupName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 1")
                it[5].equals("assignedApplicationsCount: 1")
            }
    }
    
}

