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

@Prefix("config.connection.sockettimeout") @Stepwise
class ConfigConnectionSocketTimeoutSpec extends FcliBaseSpec {
    
    def "clear"() {
        def args = "config connection sockettimeout clear"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
    def "list.empty"() {
        def args = "config connection sockettimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
    def "add"() {
        def args = "config connection sockettimeout add 10000 --name longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A10000SOCKET")
            }
    }
    
    def "verifyAdd"() {
        def args = "config connection sockettimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A10000SOCKET")
            }
    }
    
    def "update"() {
        def args = "config connection sockettimeout update 20000 --name longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A20000SOCKETUPDATED")
            }
    }
    
    def "verifyUpdate"() {
        def args = "config connection sockettimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].replace(" ", "").equals("longtimeoutN/A20000SOCKET")
            }
    }
    
    def "delete"() {
        def args = "config connection sockettimeout delete longtimeout"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.last().contains("DELETED")
            }
    }
    
    def "verifyDeleted"() {
        def args = "config connection sockettimeout list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == "No data"
            }
    }
    
}