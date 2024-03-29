package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

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
    
    @Shared FoDWebAppSupplier webApp = new FoDWebAppSupplier();
    @Shared FoDMobileAppSupplier mobileApp = new FoDMobileAppSupplier();
    @Shared FoDMicroservicesAppSupplier microservicesApp = new FoDMicroservicesAppSupplier();
    @Shared FoDWebApp
    
    def "list"() {
        def args = "fod app list --store=apps"
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
    
    def "list-scans"() {
        def args = "fod app list-scans --app=::apps::get(0).applicationId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=0
                if(size()>1) {
                    it[0].replace(' ', '').equals("IdTypeAnalysisStatusNameMicroserviceReleaseStartedCompletedScanMethod")
                } else {
                it[0].equals("No data")
                }
            }
    }
    
    def "createWebApp"() {
        when:
            webApp.get();
        then:
            noExceptionThrown()
    }
    
    def "createMicroserviceApp"() {
        when:
            microservicesApp.get();
        then:
            noExceptionThrown()
    }
    
    def "createMobileApp"() {
        when:
            mobileApp.get();
        then:
            noExceptionThrown()
    }
    
    def "get.byIdWebApp"() {
        def args = "fod app get " + webApp.get().get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + webApp.get().appName + "\"")
            }
    }
    
    def "get.byIdMobileApp"() {
        def args = "fod app get " + mobileApp.get().get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + mobileApp.get().appName + "\"")
            }
    }
    
    def "get.byIdMicroservicesApp"() {
        def args = "fod app get " + microservicesApp.get().get("applicationId")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("applicationName: \"" + microservicesApp.get().appName + "\"")
            }
    }
    
    def "get.byNameWebApp"() {
        def args = "fod app get " + webApp.get().appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + webApp.get().get("applicationId"))
            }
    }
    
    def "get.byNameMobileApp"() {
        def args = "fod app get " + mobileApp.get().appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + mobileApp.get().get("applicationId"))
            }
    }
    
    def "get.byNameMicroservicesApp"() {
        def args = "fod app get " + microservicesApp.get().appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("applicationId: " + microservicesApp.get().get("applicationId"))
            }
    }
    
    
    def "update"() {
        def args = "fod app update " + webApp.get().appName + " --business-criticality High"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod app get " + webApp.get().get("applicationId")
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
                !it.any { it.contains(webApp.get().appName) }
                !it.any { it.contains(microservicesApp.get().appName) }
                !it.any { it.contains(mobileApp.get().appName) }
            }
    }
    
    
}

