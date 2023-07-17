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
    @Shared @AutoCleanup def SSCAppVersion version = new SSCAppVersion().create()
    
    def "list"() {
        def args = ["ssc", "app", "list"]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ version.appName }
            }
    }
    
    def "get"() {
        def args = ["ssc", "app", "get", version.get("application.id")]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ version.appName }
            }
    }
}