package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserSupplier
import com.fortify.cli.ftest.fod._common.FoDMobileAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserGroupSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.scan") @FcliSession(FOD) @Stepwise
class FoDScanSpec extends FcliBaseSpec {
    /*
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    @Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    @Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @TestResource("runtime/shared/iwa_mobile.fpr") String mobileResults
    @Shared @TestResource("runtime/shared/EightBall-package.zip") String sastpackage
    @Shared @AutoCleanup FoDWebAppSupplier webApp = new FoDWebAppSupplier()
    @Shared @AutoCleanup FoDMobileAppSupplier mobileApp = new FoDMobileAppSupplier()

    
    
    def "import-sast"() {
        def args = "fod sast-scan import --release=${webApp.get().qualifiedRelease} --file=$sastResults --store uploadsast"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }
    
    def "import-mobile"() {
        def args = "fod mast-scan import --release=${mobileApp.get().qualifiedRelease} --file=$mobileResults --store uploadmast"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }

    def "import-dast"() {
        def args = "fod dast-scan import --release=${webApp.get().qualifiedRelease} --file=$dastResults --store uploaddast"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
    }

    def "import-oss"() {
        def args = "fod oss-scan import --release=${webApp.get().qualifiedRelease} --file=$ossResults --store uploadoss"
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
            def success = true;
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

    
    def "list.sast-scans"() {
        def args = "fod sast-scan list --release=fcli-1698140484524:v1698140484524 --store sastscans"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("FPRImport")
            }
    }
    
    def "get.sast-scan"() {
        def args = "fod sast-scan get ::sastscans::get(0).scanId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                //it.any {it.contains("applicationName: \"${webApp.get().appName}\"")}
            }
    }
    
    def "get-config.sast-scan"() {
        def args = "fod sast-scan get-config --release=fcli-1698140484524:v1698140484524"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("state: \"Not configured\"")
            }
    }
    
    def "download.sast-scan-byId"() {
        def args = "fod sast-scan download ::sastscans::get(0).scanId -f=byId.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "download-latest.sast-scan"() {
        def args = "fod sast-scan download-latest --release=fcli-1698140484524:v1698140484524 -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "start.sast-scan"() {
        def args = "fod sast-scan download-latest --release=fcli-1698140484524:v1698140484524 -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    /*
    def "import-sast"() {
        //get release id
        def appRelId = Fcli.run("fod release get " + app.appName + ":" + app.versionName + " --store release")
        def args = "fod scan import-sast ::release::releaseId -f " + sastResults + " --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("startedByUserId: ")
            }
    }

    def "wait-for-import-sast"() {
        def args = "fod scan wait-for ::upload:: -i 2s --until=all-match --any-scan-state=COMPLETED,CANCELLED,FAILED,RUNNING"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                //
            }
    }*/

    /*the manpages description of scan-id is "Scan id(s)" implying the posibility
      of providing multiple ids, this does not seem to work
    def "get.byIdMultiple"() {
        def scanId1= Fcli.run("util var contents scans -q scanId==#var('scans').get(0).scanId -o expr={scanId}");
        def scanId2= Fcli.run("util var contents scans -q scanId==#var('scans').get(1).scanId -o expr={scanId}");
        def args = "fod scan get " + scanId1.stdout[0] + "," + scanId2.stdout[0]
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("userId: ")
            }
    }*/


}

