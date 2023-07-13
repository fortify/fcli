package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.spec.BaseFcliSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.basic-info")
class BasicInfoSpec extends BaseFcliSpec {
    def "testGitHubFailure"() {
        expect:
            true==false
    }
    
    def "help"(String[] args, boolean expectSuccess) {
        expect:
            fcli(args)==expectSuccess
            out.lines
            verifyAll(out.lines) {
                it.any { it ==~ /.*Command-line interface for working with various Fortify products.*/ }
                it.any { it.contains 'config' }
                it.any { it.contains 'state' }
                it.any { it.contains 'ssc' }
                it.any { it.contains 'sc-dast' }
                it.any { it.contains 'sc-sast' }
                it.any { it.contains 'util' }
        }
        
        where:
            args   | expectSuccess
            ["-h"] | true    // Explicitly invoke fcli -h
            []     | false   // Invoke fcli without args, resulting in a 'missing required subcommand' error
    }

    def "version"() {
        expect:
            fcli "-V"
            out.lines
            verifyAll(out.lines) {
                size()==1
                it.any { it ==~ /.*fcli version \d+\.\d+\.\d+.*, built on.*/ }
            }
    }
}