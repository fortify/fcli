package com.fortify.cli.ftest.sc_sast;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SCSAST
import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared

@Prefix("sc-sast.scan") @FcliSession([SCSAST, SSC]) 
class SCSastSensorSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    
    def "help"() {
        def args = ["sc-sast", "scan", "-h"]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
}