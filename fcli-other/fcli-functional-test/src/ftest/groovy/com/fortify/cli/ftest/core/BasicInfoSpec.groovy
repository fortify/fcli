package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.basic-info")
class BasicInfoSpec extends FcliBaseSpec {
    def "help"(String[] args, boolean expectSuccess) {
        expect:
            verifyAll(Fcli.run(args)) {
                success==expectSuccess
                verifyAll(expectSuccess ? stdout : stderr) {
                    it.any { it ==~ /.*Command-line interface for working with various Fortify products.*/ }
                    it.any { it.contains 'config' }
                    it.any { it.contains 'state' }
                    it.any { it.contains 'ssc' }
                    it.any { it.contains 'sc-dast' }
                    it.any { it.contains 'sc-sast' }
                    it.any { it.contains 'util' }
                }
            }
        
        where:
            args   | expectSuccess
            ["-h"] | true    // Explicitly invoke fcli -h
            []     | false   // Invoke fcli without args, resulting in a 'missing required subcommand' error
    }

    def "version"() {
        expect:
            verifyAll(Fcli.run("-V")) {
                success
                verifyAll(stdout) {
                    size()==1
                    it[0] ==~ /.*fcli version \d+\.\d+\.\d+.*, built on.*/
                }
            }
    }
}