package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

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

@Prefix("fod.role") @FcliSession(FOD) @Stepwise
class FoDAccessControlRoleSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDUserSupplier user = new FoDUserSupplier()
    @Shared @AutoCleanup FoDUserGroupSupplier group = new FoDUserGroupSupplier()
    @Shared @AutoCleanup FoDWebAppSupplier app = new FoDWebAppSupplier()
    
    def "list"() {
        def args = "fod ac list-roles --store roles"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("IdName")
            }
    }
    
    def "updateUserRole"() {
        def args = "fod ac update-user ${user.get().userName} --lastname updatedLastname --firstname updatedFirstname --phone 5678 --role=Developer"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        
        def args = "fod ac get-user ${user.get().userName}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[9].equals("roleName: \"Developer\"")
            }
    }
    
    
}

