package com.fortify.cli.ftest.config;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("config.truststore") @Stepwise
class ConfigTrustStoreSpec extends FcliBaseSpec {
    @Shared @TempDir("sc-client") String scClientInstallDir;
    @Shared @TestResource("runtime/config/dummyStore.jks") String dummyStore;
    
    def setupSpec() {
        Fcli.run("config truststore clear")
    }
    
    def cleanupSpec() {
        Fcli.run("config truststore clear")
    }
    
    def "get.empty"() {
        def args = "config truststore get"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it == "path: null" }
            }
    }
    
    def "set"() {
        def args = "config truststore set ${dummyStore}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ /CONFIGURED/ }
            }
    }
    
    def "get.not-empty"() {
        def args = "config truststore get"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                // TODO
            }
    }
    
    def "install.failure"() {
        def args = "tool sc-client install -y -d ${scClientInstallDir}"
        when:
            def result = Fcli.run(args, {it.expectSuccess(false)})
        then:
            verifyAll(result.stderr) {
                it.any { it.contains("javax.net.ssl.SSLException") }
            }
    }
    
    def "clear"() {
        def args = "config truststore clear"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                // TODO
            }
    }
}