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

@Prefix("config.connection.connecttimeout") @Stepwise
class ConfigConnectionConnectTimeoutSpec extends FcliBaseSpec {
    
    def "clear"() {
        def args = "config connection connecttimeout clear"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
    def "list.empty"() {
        def args = "config connection connecttimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
    def "add"() {
        def args = "config connection connecttimeout add 10000 --name longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A10000CONNECT")
            }
    }
    
    def "verifyAdd"() {
        def args = "config connection connecttimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A10000CONNECT")
            }
    }
    
    def "update"() {
        def args = "config connection connecttimeout update 20000 --name longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A20000CONNECTUPDATED")
            }
    }
    
    def "verifyUpdate"() {
        def args = "config connection connecttimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A20000CONNECT")
            }
    }
    
    def "delete"() {
        def args = "config connection connecttimeout delete longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.last().contains("DELETED")
            }
    }
    
    def "verifyDeleted"() {
        def args = "config connection connecttimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
}