package com.fortify.cli.functest.fcli;

import com.fortify.cli.functest.util.FcliSpec

class FcliRootSpec extends FcliSpec {
    def "fcli"(String[] args, boolean success) {
        expect:
        fcli(args)==success
        out.lines
        verifyAll(out.lines) {
            it.any { it ==~ /.*Command-line interface for working with various Fortify products..*/ }
            it.any { it.contains 'config' }
            it.any { it.contains 'state' }
            it.any { it.contains 'ssc' }
            it.any { it.contains 'sc-dast' }
            it.any { it.contains 'sc-sast' }
            it.any { it.contains 'util' }
        }
        
        where:
        args   | success
        ["-h"] | true    // Explicitly invoke fcli -h
        []     | false   // Invoke fcli without args, resulting in a 'missing required subcommand' error
    }

    def "fcli -V"() {
        expect:
        fcli "-V"
        out.lines
        verifyAll(out.lines) {
            size()==1
            it.any { it ==~ /.*fcli version \d+\.\d+\.\d+, built on.*/ }
        }
    }
}