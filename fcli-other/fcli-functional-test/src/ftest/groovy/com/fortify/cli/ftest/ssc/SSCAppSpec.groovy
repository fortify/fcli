package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Unroll

@Prefix("ssc.app") @FcliSession(SSC)
class SSCAppSpec extends FcliBaseSpec {
    @Shared @AutoCleanup def SSCAppVersion version = new SSCAppVersion().create()
    def "list"() {
        expect:
            fcli("ssc", "app", "list")
            out.lines.any { it =! version.appName }
    }
}