package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion
import com.fortify.cli.ftest.ssc._common.SSCUser
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion-user") @FcliSession(SSC) @Stepwise
class SSCAppVersionUserSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    @Shared @AutoCleanup SSCUser user = new SSCUser().create()
    
    def "list"() {
        def args = "ssc appversion-user list --appversion " + version.appName + ":" + version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=1
                if(size()>1) {
                    it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldap");
                } else {
                    it[0].equals("No data");
                }
                
            }
    }
    
    def "add"() {
        def args = "ssc appversion-user add " + user.userName + " --appversion " + version.appName + ":" + version.versionName 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldapAction");
                it[1].contains(user.userName)
            }
    }
    
    def "verifyAdd"() {
        def args = "ssc appversion-user list --appversion " + version.appName + ":" + version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldap");
                it[1].contains(user.userName)
            }
    }
    
    def "delete"() {
        def args = "ssc appversion-user delete " + user.userName + " --appversion " + version.appName + ":" + version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldapAction");
                it[1].contains("DELETED")
            }
    }
    
    def "verifyDelete"() {
        def args = "ssc appversion-user list --appversion " + version.appName + ":" + version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0].equals("No data");
            }
    }
}