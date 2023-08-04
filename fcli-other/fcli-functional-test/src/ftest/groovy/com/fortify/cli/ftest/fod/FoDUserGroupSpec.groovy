package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDAppRel
import com.fortify.cli.ftest.fod._common.FoDUser
import com.fortify.cli.ftest.fod._common.FoDUserGroup
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.app") @FcliSession(FOD) @Stepwise
class FoDUserGroupSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDUser user = new FoDUser().create()
    @Shared @AutoCleanup FoDUserGroup group = new FoDUserGroup().create()
    @Shared @AutoCleanup FoDAppRel app = new FoDAppRel().createWebApp()
    
    def "list"() {
        def args = "fod user-group list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("IdNameAssignedUsersAssignedApplications")
            }
    }
    
    def "get.byName"() {
        def args = "fod user-group get " + group.groupName + " --store group" 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=6
                it[2].contains(group.groupName)
            }
    }
    
    def "get.byId"() {
        def args = "fod user-group get ::group::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=6
                it[2].contains(group.groupName)
            }
    }
    
    
    def "update"() {
        def args = "fod user-group update " + group.groupName + 
                    " --add-users " + user.userName +
                    " --add-apps " + app.appName
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
        def args = "fod user-group get " + group.groupName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[4].equals("assignedUsersCount: 1")
                it[5].equals("assignedApplicationsCount: 1")
            }
    }
    
}

