package com.fortify.cli.ftest.core

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD
import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDReleaseSupplier
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.Shared

@Prefix("core.action.with-product") @FcliSession([SSC, FOD])
class ActionWithProductSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/with-product.yaml") String actionPath
    @Global(SSCAppVersionSupplier.EightBall.class) SSCAppVersionSupplier sscVersionSupplier
    @Global(FoDReleaseSupplier.EightBall.class) FoDReleaseSupplier fodReleaseSupplier

    def "with.product ssc: REST helper accessible"() {
        when:
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode ssc-rest")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("SSC-REST-OK") }
            }
    }

    def "with.product ssc: SpEL function #ssc.appVersion accessible"() {
        when:
            def avId = sscVersionSupplier.version.get("id")
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode ssc-spel --id ${avId}")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("SSC-SPEL-OK") }
            }
    }

    def "with.product fod: REST helper accessible"() {
        when:
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fod-rest")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("FOD-REST-OK") }
            }
    }

    def "with.product fod: SpEL function #fod.release accessible"() {
        when:
            def relId = fodReleaseSupplier.release.get("releaseId")
            def result = Fcli.run("action run ${actionPath} --progress=none --on-unsigned=ignore --on-invalid-version=ignore --mode fod-spel --id ${relId}")
        then:
            verifyAll(result.stdout) {
                it.any { it.startsWith("FOD-SPEL-OK") }
            }
    }
}
