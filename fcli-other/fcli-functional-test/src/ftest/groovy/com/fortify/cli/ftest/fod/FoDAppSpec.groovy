package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDMicroservicesAppSupplier
import com.fortify.cli.ftest.fod._common.FoDMobileAppSupplier
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier

import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("fod.app") @FcliSession(FOD) @Stepwise
class FoDAppSpec extends FcliBaseSpec {
    /*
    @Shared FoDWebAppSupplier webAppSupplier = null;
    @Shared FoDMobileAppSupplier mobileAppSupplier = null;
    @Shared FoDMicroservicesAppSupplier microservicesAppSupplier = null;
    
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
            webApp = new FoDWebAppSupplier().createWebApp();
        then:
            noExceptionThrown()
    }
    
    def "createMicroserviceApp"() {
        when:
            microservicesApp = new FoDWebAppSupplier().createMicroservicesApp();
        then:
            noExceptionThrown()
    }
    
    def "createMobileApp"() {
        when:
            mobileApp = new FoDWebAppSupplier().createMobileApp();
        then:
            noExceptionThrown()
    }
    
    def "get.byIdWebApp"() {
        def args = "fod app get " + webApp.get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + webApp.appName + ":" + webApp.versionName +"\"")
            }
    }
    
    def "get.byIdMobileApp"() {
        def args = "fod app get " + mobileApp.get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + mobileApp.appName + ":" + mobileApp.versionName +"\"")
            }
    }
    
    def "get.byIdMicroservicesApp"() {
        def args = "fod app get " + microservicesApp.get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + microservicesApp.appName + ":" + microservicesApp.versionName +"\"")
            }
    }
    
    def "get.byNameWebApp"() {
        def args = "fod app get " + webApp.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + webApp.get("applicationId"))
            }
    }
    
    def "get.byNameMobileApp"() {
        def args = "fod app get " + mobileApp.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + mobileApp.get("applicationId"))
            }
    }
    
    def "get.byNameMicroservicesApp"() {
        def args = "fod app get " + microservicesApp.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + microservicesApp.get("applicationId"))
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
        def args = "fod app get " + webApp.get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[6].equals("businessCriticalityType: \"High\"")
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
    */
    
}

