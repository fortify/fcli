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

@Prefix("config.proxy") @Stepwise
class ConfigProxySpec extends FcliBaseSpec {
    @Shared @TempDir("sc-client") String scClientInstallDir;
    
    def setupSpec() {
        Fcli.run("config proxy clear")
    }
    
    def cleanupSpec() {
        Fcli.run("config proxy clear")
    }
    
    def "list.empty"() {
        def args = "config proxy list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
    def "add"() {
        def args = "config proxy add dummy-server-9482:32000"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ /dummy-server-9482:32000/ }
            }
    }
    
    def "list.not-empty"() {
        def args = "config proxy list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ /dummy-server-9482:32000/ }
            }
    }
    
    def "install.failure"() {
        def args = "tool sc-client install -y -d ${scClientInstallDir}"
        when:
            def result = Fcli.run(args, {it.expectSuccess(false)})
        then:
            verifyAll(result.stderr) {
                it.any { it.contains("java.net.UnknownHostException: dummy-server-9482") }
            }
    }
    
    def "rm"() {
        def args = "config proxy rm dummy-server-9482:32000"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ /dummy-server-9482:32000/ }
            }
    }
    
}