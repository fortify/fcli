package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared

@Prefix("ssc.app") @FcliSession(SSC)
class SSCAppSpec extends FcliBaseSpec {
    @Shared SSCAppVersion version = new SSCAppVersion().create()
    
    def "list"() {
        def args = "ssc app list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ version.appName }
            }
    }
    
    def "get.byId"() {
        def args = "ssc app get "+version.get("application.id")
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ version.appName }
            }
    }
    
    def "get.byName"() {
        def args = "ssc app get "+version.appName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ version.appName }
            }
    }
    
    def "update"() {
        def args = "ssc app update "+version.appName + " --description updateddescription"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "updateddescription" }
            }
    }
    
    def "delete"() {
        def args = "ssc app delete "+version.appName +" -y"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "DELETED" }
            }
    }
    
    /*
    def "delete-all"() {
        when:
            def SSCAppVersion version1 = new SSCAppVersion().create()
            def SSCAppVersion version2 = new SSCAppVersion().create(v1.appName)
            def deleteResult = Fcli.run(["ssc", "app", "rm", version.appName, "--confirm"])
            def listResult = Fcli.run(["ssc", "app", "list"])
        then:
            verifyAll(deleteResult.stdout) {
                it.any { it =~ version.appName }
            }
    }
    */
}