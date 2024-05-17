package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempFile
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.artifact") @FcliSession(SSC) @Stepwise
class SSCArtifactUploadSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier = new SSCAppVersionSupplier()
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String fpr
    @Shared @TestResource("runtime/shared/LoginProject.fpr") String diffpr
    @Shared String uploadVariableName = versionSupplier.version.fcliVariableName+"_artifact"
    @Shared String uploadVariableRef = "::$uploadVariableName::"
    @Shared @TempFile("artifactUploadSpec/download.fpr") String downloadedFpr;
    
    def "upload"() {
        def args = "ssc artifact upload -f $fpr --appversion "+ 
            "${versionSupplier.version.variableRef} --store $uploadVariableName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[0] =~ /Id\s+Scan types\s+Last scan date\s+Upload date\s+Status/
                it[1] =~ /^\s*\d+.*/
            }
    }
    
    def "wait-for"() {
        // Depending on externalmetadata versions in FPR and on SSC, approval
        // may be required
        def args = "ssc artifact wait-for $uploadVariableRef -i 2s -s PROCESS_COMPLETE,REQUIRE_AUTH"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "WAIT_COMPLETE" }
            }
    }
    
    def "approve-first"() {
        def args = "ssc artifact approve ::$uploadVariableName::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "NO_APPROVAL_NEEDED" || it =~ "APPROVED" }
            }
    }
    
    
    def "upload-dif"() {
        def args = "ssc artifact upload -f $diffpr --appversion "+
            "${versionSupplier.version.variableRef} --store upload"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[0] =~ /Id\s+Scan types\s+Last scan date\s+Upload date\s+Status/
                it[1] =~ /^\s*\d+.*/
            }
    }
    
    def "wait-for-dif"() {
        // Depending on externalmetadata versions in FPR and on SSC, approval
        // may be required
        def args = "ssc artifact wait-for ::upload:: -i 2s -s PROCESS_COMPLETE,REQUIRE_AUTH"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "WAIT_COMPLETE" }
            }
    }
    
    def "approve-dif"() {
        def args = "ssc artifact approve ::upload::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "APPROVED" }
            }
    }
    
    def "wait-for-dif-process"() {
        // Depending on externalmetadata versions in FPR and on SSC, approval
        // may be required
        def args = "ssc artifact wait-for ::upload:: -i 2s -s PROCESS_COMPLETE"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "WAIT_COMPLETE" }
            }
    }
    
    def "list"() {
        def args = "ssc artifact list --appversion ${versionSupplier.version.variableRef}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[0].replace(" ", "").equals("IdScantypesLastscandateUploaddateStatus")
            }
    }
    
    def "get"() {
        def args = "ssc artifact get $uploadVariableRef"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.equals("originalFileName: EightBall-22.1.0.fpr") }
            }
    }
    
    def "download"() {
        def args = "ssc artifact download ::upload::id -f ${downloadedFpr} --no-include-sources"
        when:
            def result = Fcli.run(args)
        then:
            noExceptionThrown()
            new File(downloadedFpr).exists()
            verifyAll(result.stdout) {
                it.last().contains("ARTIFACT_DOWNLOADED");
            }
    }
    
    def "getIssueCount"() {
        def args = "ssc issue count --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
        when:
            def result = Fcli.run(args)
        then:
            noExceptionThrown()
            verifyAll(result.stdout) {
                it.last().replace(" ","").contains("Low17");
            }
    }
    
    def "getIssueCountQuickView"() {
        def args = "ssc issue count --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName + " --filterset Quick\\ View"
        when:
            def result = Fcli.run(args)
        then:
            noExceptionThrown()
            verifyAll(result.stdout) {
                it.last().replace(" ","").contains("High4");
            }
    }
    
    @Requires({System.getProperty('ft.debricked.user') && System.getProperty('ft.debricked.password') && System.getProperty('ft.debricked.repository') && System.getProperty('ft.debricked.branch')})
    def "import-debricked"() {
        def args = "ssc artifact import-debricked --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName +
                    " --repository " + System.getProperty("ft.debricked.repository") + 
                    " --branch " + System.getProperty("ft.debricked.branch") + 
                    " --debricked-user " + System.getProperty("ft.debricked.user") + 
                    " --debricked-password " + System.getProperty("ft.debricked.password") +
                    " --store debricked"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[3] =~ /Id\s+Scan types\s+Last scan date\s+Upload date\s+Status/
                it[4] =~ /^\s*\d+.*/
            }
    }
    
    @Requires({System.getProperty('ft.debricked.user') && System.getProperty('ft.debricked.password') && System.getProperty('ft.debricked.repository') && System.getProperty('ft.debricked.branch')})
    def "wait-for-debricked"() {
        // Depending on externalmetadata versions in FPR and on SSC, approval
        // may be required
        def args = "ssc artifact wait-for ::debricked:: -i 2s -s PROCESS_COMPLETE,REQUIRE_AUTH"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "WAIT_COMPLETE" }
            }
    }
    
    def "purge.byId"() {
        //Thread.sleep(10000);
        def args = "ssc artifact purge ::$uploadVariableName::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "PURGE_REQUESTED" }
            }
    }
    
}