package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDMobileAppSupplier
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("fod.scan") @FcliSession(FOD) @Stepwise
class FoDScanSpec extends FcliBaseSpec {
    
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    @Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    @Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @TestResource("runtime/shared/iwa_mobile.fpr") String mobileResults
    @Shared @TestResource("runtime/shared/EightBall-package.zip") String sastPackage
    @Shared @TestResource("runtime/shared/HelloWorld.apk") String mastPackage
    @Shared @TestResource("runtime/shared/oss_package.zip") String ossPackage
    @Shared @AutoCleanup FoDWebAppSupplier webApp = new FoDWebAppSupplier()
    @Shared @AutoCleanup FoDMobileAppSupplier mobileApp = new FoDMobileAppSupplier()

    
    /*
    def "import-sast"() {
        def args = "fod sast-scan import --release=${webApp.get().qualifiedRelease} --file=$tar --store uploadsast"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it.last().contains("IMPORT_REQUESTED")
            }
        cleanup:
            Files.delete(tar);
    }
    /*
    def "import-mast"() {
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
                if(result.stdout.findAll{
                    element -> element.contains("analysisStatusType: \"Completed\"")}.size()==4) {
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
        def args = "fod sast-scan list --release=${webApp.get().qualifiedRelease} --store sastscans"
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
    
    def "setup.sast-scan"() {
        def args = "fod sast-scan setup --assessment-type=Static\\ Assessment --audit-preference=Automated --frequency=SingleScan --technology-stack=Go --release=${webApp.get().qualifiedRelease}"
        when:
            def result = Fcli.run(args)
        then:
            def e = thrown(UnexpectedFcliResultException)
            e.result.stderr.any { it.contains("the entitlement has expired") }
            e.result.stdout.first().replace(" ", "").equals("AssessmenttypeidEntitlementidEntitlementfrequencytypeReleaseidTechnologystackidTechnologystackLanguagelevelidLanguagelevelOSSAnalysisAuditpreferencetypeIncludethirdpartylibrariesUsesourcecontrolScanbinaryBsitokenApplicationReleaseMicroserviceAction")
    }
    
    def "get-config.sast-scan"() {
        def args = "fod sast-scan get-config --release=${webApp.get().qualifiedRelease}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("state: \"Configured\"")
            }
    }
    /*
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
        def args = "fod sast-scan download-latest --release=${webApp.get().qualifiedRelease} -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "list.dast-scans"() {
        def args = "fod dast-scan list --release=${webApp.get().qualifiedRelease} --store dastscans"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("FPRImport")
            }
    }
    
    def "get.dast-scan"() {
        def args = "fod dast-scan get ::dastscans::get(0).scanId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                //it.any {it.contains("applicationName: \"${webApp.get().appName}\"")}
            }
    }
    
    def "get-config.dast-scan"() {
        def args = "fod dast-scan get-config --release=${webApp.get().qualifiedRelease}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("state: \"Not configured\"")
            }
    }
    
    def "download.dast-scan-byId"() {
        def args = "fod dast-scan download ::dastscans::get(0).scanId -f=byId.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "download-latest.dast-scan"() {
        def args = "fod dast-scan download-latest --release=${webApp.get().qualifiedRelease} -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "list.mast-scans"() {
        def args = "fod mast-scan list --release=${mobileApp.get().qualifiedRelease} --store mastscans"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("FPRImport")
            }
    }
    
    def "get.mast-scan"() {
        def args = "fod mast-scan get ::mastscans::get(0).scanId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                //it.any {it.contains("applicationName: \"${webApp.get().appName}\"")}
            }
    }
    
    def "get-config.mast-scan"() {
        def args = "fod mast-scan get-config --release=${mobileApp.get().qualifiedRelease}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("state: \"Not configured\"")
            }
    }
    
    def "download.mast-scan-byId"() {
        def args = "fod mast-scan download ::mastscans::get(0).scanId -f=byId.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "download-latest.mast-scan"() {
        def args = "fod mast-scan download-latest --release=${mobileApp.get().qualifiedRelease} -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "list.oss-scans"() {
        def args = "fod oss-scan list --release=${webApp.get().qualifiedRelease} --store ossscans"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("FPRImport")
            }
    }
    
    def "get.oss-scan"() {
        def args = "fod oss-scan get ::ossscans::get(0).scanId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                //it.any {it.contains("applicationName: \"${webApp.get().appName}\"")}
            }
    }
    
    def "get-config.oss-scan"() {
        def args = "fod oss-scan get-config --release=${webApp.get().qualifiedRelease}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("state: \"Not configured\"")
            }
    }
    
    def "download.oss-scan-byId"() {
        def args = "fod oss-scan download ::ossscans::get(0).scanId -f=byId.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "download-latest.oss-scan"() {
        def args = "fod oss-scan download-latest --release=${webApp.get().qualifiedRelease} -f=latest.fpr"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("SCAN_DOWNLOADED")
            }
    }
    
    def "start.sast-scan"() {
        def args = "fod sast-scan start --release=fcli-1698140484524:v2 --file=$sastPackage --store sastScan"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("STARTED")
            }
    }

    def "wait-for-sast"() {
        def args = "fod sast-scan wait-for ::sastScan:: -i 2s --until=all-match --any-state=Completed,In_Progress,Queued"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                //
            }
    }
    
    def "start.oss-scan"() {
        def args = "fod oss-scan start --release=fcli-1698140484524:v2 --file=$ossPackage --store ossScan"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("STARTED")
            }
    }

    def "wait-for-oss"() {
        def args = "fod oss-scan wait-for ::ossScan:: -i 2s --until=all-match --any-state=Completed,In_Progress,Queued"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                //
            }
    }
    
    def "start.mast-scan"() {
        def args = "fod mast-scan start --release=fcli-mobile:m1 --file=$mastPackage --assessment-type=Mobile\\ Assessment --framework=Android --frequency=Subscription --store mastScan"
        when:
            def result=null;
            try {
                result = Fcli.run(args)
            } catch(UnexpectedFcliResultException e) {
                if(e.result.stderr.any { it.contains("the entitlement has expired") }) {
                    result = e.result;
                } else {
                    throw e;
                }
            }
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("STARTED")
            }
    }

    def "wait-for-mast"() {
        def args = "fod mast-scan wait-for ::mastScan:: -i 2s --until=all-match --any-state=Completed,In_Progress,Queued"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                //
            }
    }
    /* currently not implemented, awaiting availability of automated dast scan on fod
    def "start.dast-scan"() {
        def args = "fod dast-scan start --release=fcli-1698140484524:v2 --store dastScan"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("STARTED")
            }
    }

    def "wait-for-dast"() {
        def args = "fod dast-scan wait-for ::dastScan:: -i 2s --until=all-match --any-state=Completed,In_Progress,Queued"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                //
            }
    }*/


}

