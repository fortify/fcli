package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.basic-info")
class BasicInfoSpec extends FcliBaseSpec {
    def "help"(List<String> args, boolean expectedSuccess) {
        when:
            def result = Fcli.run(args, {it.expectSuccess(expectedSuccess)})
        then:
            verifyAll(expectedSuccess ? result.stdout : result.stderr) {
                it.any { it ==~ /.*Command-line interface for working with various Fortify products.*/ }
                it.any { it.contains 'config' }
                it.any { it.contains 'state' }
                it.any { it.contains 'ssc' }
                it.any { it.contains 'sc-dast' }
                it.any { it.contains 'sc-sast' }
                it.any { it.contains 'util' }
            }
        where:
            args   | expectedSuccess
            ["-h"] | true    // Explicitly invoke fcli -h
            []     | false   // Invoke fcli without args, resulting in a 'missing required subcommand' error
    }

    def "version"() {
        def args = ["-V"]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] ==~ /.*fcli version \d+\.\d+\.\d+.*, built on.*/
            }
    }
}