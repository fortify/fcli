package com.fortify.cli.ftest.core

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

import spock.lang.Stepwise

@Prefix("core.ai-assist.extensions") @Stepwise
class AiAssistExtensionsSpec extends FcliBaseSpec {

    def "list-installed"() {
        when:
            def result = Fcli.run("ai-assist extensions list-installed")
        then:
            verifyAll(result.stdout) {
                size() == 1
                it[0].trim() == "No data"
            }
    }
}
