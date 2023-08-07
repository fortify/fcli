package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.fod._common.FoDApp
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.release") @FcliSession(FOD) @Stepwise
class FoDReleaseSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FoDApp app = new FoDApp().createMicroservicesApp()
    
    def "list"() {
        def args = "fod release list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=1
                if(size()>1) {
                    it[0].replace(' ', '').equals("IdNameMicroserviceApplicationSDLCStatus")
                } else {
                    it[0].equals("No data")
                }
            }
    }
    
    def "create"() {
        def args = "fod release create " + app.appName + ":" + app.microserviceName + ":testrel --sdlc-status=Development --store testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
            }
    }
    
    def "verifyCreated"() {
        def args = "fod release list --store releases"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(app.versionName) }
            }
    }
    
    def "get.byId"() {
        def args = "fod release get ::testrel::releaseId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("releaseName: \"testrel\"")
                it[9].equals("applicationName: \"" + app.appName + ":" + app.versionName + "\"")
            }
    }
    
    def "get.byName"() {
        def args = "fod release get " + app.appName + ":" + app.microserviceName + ":testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("releaseName: \"testrel\"")
                it[9].equals("applicationName: \"" + app.appName + ":" + app.versionName + "\"")
            }
    }
    
    
    def "update"() {
        def args = "fod release update "  + app.appName + ":" + app.microserviceName + ":testrel --sdlc-status QA"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod release get ::testrel::releaseId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any {it.equals("sdlcStatusType: \"QA\"") }
            }
    }
    
    def "delete"() {
        def args = "fod release delete " + app.appName + ":testrel"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyDeleted"() {
        def args = "fod release list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains("testrel") }
            }
    }
    
}

