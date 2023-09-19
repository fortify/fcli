package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDMobileAppSupplier
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import spock.lang.AutoCleanup
import spock.lang.Shared


@Prefix("fod.import-scan") @FcliSession(FOD)
class FoDReleaseImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    @Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    @Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @TestResource("runtime/shared/iwa_mobile.fpr") String mobileResults
    @Shared @AutoCleanup FoDWebAppSupplier webApp = new FoDWebAppSupplier()
    @Shared @AutoCleanup FoDMobileAppSupplier mobileApp = new FoDMobileAppSupplier()

    
    def "import-mobile"() {
        def args = "fod release import-mobile --release ${mobileApp.get().qualifiedRelease} $mobileResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-sast"() {
        def args = "fod release import-sast --release ${webApp.get().qualifiedRelease} $sastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-dast"() {
        def args = "fod release import-dast --release ${webApp.get().qualifiedRelease} $dastResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-oss"() {
        def args = "fod release import-oss --release ${webApp.get().qualifiedRelease} $ossResults --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "waitForScans"() {
        when:
            def relScanurl = Fcli.run("fod release get ${webApp.get().qualifiedRelease} -o expr=/api/v3/releases/{releaseId}/scans --store relId").stdout[0]
            def timeoutMs = 60000
            def start = System.currentTimeMillis()
            def success = false;
            while(true){
                def result = Fcli.run("fod rest call ${relScanurl}")
                if(result.stdout.findAll{element -> element.contains("analysisStatusType: \"Completed\"")}.size()==3) {
                    success=true;
                    break;
                } else if(System.currentTimeMillis()-start > timeoutMs) {
                    break;
                }
                sleep(3000)
            }
        then:
            success
    }
    
    def "waitForScans2"() {
        when:
            def relScanurl = Fcli.run("fod release get ${mobileApp.get().qualifiedRelease} -o expr=/api/v3/releases/{releaseId}/scans --store relId").stdout[0]
            def timeoutMs = 60000
            def start = System.currentTimeMillis()
            def success = false;
            while(true){
                def result = Fcli.run("fod rest call ${relScanurl}")
                if(result.stdout.findAll{element -> element.contains("analysisStatusType: \"Completed\"")}.size()==1) {
                    success=true;
                    break;
                } else if(System.currentTimeMillis()-start > timeoutMs) {
                    break;
                }
                sleep(3000)
            }
        then:
            success
    }
    
}

