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

@Prefix("fod.microservice") @FcliSession(FOD) @Stepwise
class FoDMicroserviceSpec extends FcliBaseSpec {
    @Shared @AutoCleanup FODAppRel app = new FODAppRel().createMicroservicesApp()
    
    @Shared 
    boolean appsExist = false;
    
    def "list"() {
        def args = "fod microservice list --app=" + app.appName
        when:
            def result = Fcli.run(args)
            appsExist = result.stdout.size()>1
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains(app.appName)
            }
    }
    
    def "create"() {
        def args = "fod microservice create " + app.appName + ":testservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
            }
    }
    
    def "verifyCreated"() {
        def args = "fod microservice list --app=" + app.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("testservice") }
            }
    }
    
    def "update"() {
        def args = "fod microservice update "  + app.appName + ":testservice --name=updatedtestservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyUpdated"() {
        def args = "fod microservice list --app=" + app.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains(app.appName) && it.contains("updatedtestservice") }
            }
    }
    
    def "delete"() {
        def args = "fod microservice delete " + app.appName + ":updatedtestservice"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }
    
    def "verifyDeleted"() {
        def args = "fod microservice list --app=" + app.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                !it.any { it.contains("updatedtestservice") }
            }
    }
    
}

