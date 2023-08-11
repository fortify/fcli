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
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.import-scan") @FcliSession(FOD) @Stepwise
class FoDImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    @Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    @Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @TestResource("runtime/shared/iwa_mobile.fpr") String mobileResults
    @Shared @AutoCleanup FoDApp app = new FoDApp().createWebApp()

    
    def "import-sast"() {
        //get release id
        def appRelId = Fcli.run("fod release get " + app.appName + ":" + app.versionName + " --store release")
        def args = "fod scan-import import-sast --release ::release::releaseId $sastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-dast"() {
        def args = "fod scan-import import-dast --release ::release::releaseId $dastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-oss"() {
        def args = "fod scan-import import-oss --release ::release::releaseId $ossResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-mobile"() {
        def args = "fod scan-import import-mobile --release ::release::releaseId $mobileResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
}

