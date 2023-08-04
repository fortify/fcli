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

@Prefix("fod.app") @FcliSession(FOD) @Stepwise
class FoDAppSpec extends FcliBaseSpec {
    @Shared FODAppRel webApp = null;
    @Shared FODAppRel mobileApp = null;
    @Shared FODAppRel microservicesApp = null;
    
    def "list"() {
        def args = "fod app list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=1
                if(size()>1) {
                    it[0].replace(' ', '').equals("IdNameTypeCriticality")
                } else {
                    it[0].equals("No data")
                }
            }
    }
    
    def "createWebApp"() {
        when:
            webApp = new FODAppRel().createWebApp();
        then:
            noExceptionThrown()
    }
    
    def "createMicroserviceApp"() {
        when:
            microservicesApp = new FODAppRel().createMicroservicesApp();
        then:
            noExceptionThrown()
    }
    
    def "createMobileApp"() {
        when:
            mobileApp = new FODAppRel().createMobileApp();
        then:
            noExceptionThrown()
    }
    
    def "verifyCreated"() {
        def args = "fod app list --store apps"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(webApp.appName) }
                it.any { it.contains(microservicesApp.appName) }
                it.any { it.contains(mobileApp.appName) }
            }
    }
    
    def "get.byId"() {
        def args = "fod app get ::apps::get(0).applicationId"
        when:
            if(!appsExist) {return;}
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: ")
            }
    }
    
    def "get.byName"() {
        def args = "fod app get ::apps::get(0).applicationName"
        when:
            if(!appsExist) {return;}
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: ")
            }
    }
    
    
    def "update"() {
        def args = "fod app update " + webApp.appName + " --business-criticality High"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod app list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(webApp.appName) && it.contains("High") }
            }
    }
    
    def "deleteWebApp"() {
        when:
            webApp.close()
        then:
            noExceptionThrown()
    }
    
    def "deleteMicroserviceApp"() {
        when:
            microservicesApp.close()
        then:
            noExceptionThrown()
    }
    
    def "deleteMobileApp"() {
        when:
            mobileApp.close()
        then:
            noExceptionThrown()
    }
    
    def "verifyDeleted"() {
        def args = "fod app list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains(webApp.appName) }
                !it.any { it.contains(microservicesApp.appName) }
                !it.any { it.contains(mobileApp.appName) }
            }
    }
    
}

