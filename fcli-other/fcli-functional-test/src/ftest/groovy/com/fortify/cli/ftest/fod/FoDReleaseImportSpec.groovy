package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDApp
import com.fortify.cli.ftest.fod._common.FoDUser
import com.fortify.cli.ftest.fod._common.FoDUserGroup

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.import-scan") @FcliSession(FOD)
class FoDReleaseImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    @Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    @Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @TestResource("runtime/shared/iwa_mobile.fpr") String mobileResults
    @Shared @AutoCleanup FoDApp webApp = new FoDApp().createWebApp()
    @Shared @AutoCleanup FoDApp mobileApp = new FoDApp().createMobileApp()

    
    def "import-sast"() {
        def args = "fod release import-sast --release ${webApp.qualifiedRelease} $sastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-dast"() {
        def args = "fod release import-dast --release ${webApp.qualifiedRelease} $dastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-oss"() {
        def args = "fod release import-oss --release ${webApp.qualifiedRelease} $ossResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-mobile"() {
        def args = "fod release import-mobile --release ${mobileApp.qualifiedRelease} $mobileResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
}

