package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier
import com.fortify.cli.ftest.ssc._common.SSCLocalUserSupplier
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion-user") @FcliSession(SSC) @Stepwise
class SSCAccessControlAppVersionUserSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier = new SSCAppVersionSupplier()
    @Shared @AutoCleanup SSCLocalUserSupplier userSupplier = new SSCLocalUserSupplier()
    
    def "list"() {
        def args = "ssc ac list-appversion-users --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
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
        def args = "ssc appversion update ${versionSupplier.version.appName}:${versionSupplier.version.versionName} --add-users ${userSupplier.user.userName}" 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                // TODO Change command output or expected output?
                //it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldapAction");
                //it[1].contains(userSupplier.user.userName)
            }
    }
    
    def "verifyAdd"() {
        def args = "ssc ac list-appversion-users --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldap");
                it[1].contains(userSupplier.user.userName)
            }
    }
    
    def "delete"() {
        def args = "ssc appversion update ${versionSupplier.version.appName}:${versionSupplier.version.versionName} --rm-users ${userSupplier.user.userName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                // TODO Change command output or expected output?
                //it[0].replace(" ","").equals("IdEntitynameDisplaynameTypeEmailIsldapAction");
                //it[1].contains("DELETED")
            }
    }
    
    def "verifyDelete"() {
        def args = "ssc ac list-appversion-users --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains(userSupplier.user.userName) }
            }
    }
}