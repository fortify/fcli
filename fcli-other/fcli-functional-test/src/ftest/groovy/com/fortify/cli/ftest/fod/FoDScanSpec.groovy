package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.fod._common.FoDWebAppSupplier
import com.fortify.cli.ftest.fod._common.FoDUserSupplier
import com.fortify.cli.ftest.fod._common.FoDUserGroupSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Prefix("fod.scan") @FcliSession(FOD) @Stepwise
class FoDScanSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String sastResults
    //@Shared @TestResource("runtime/shared/iwa_net_scandata.fpr") String dastResults
    //@Shared @TestResource("runtime/shared/iwa_net_cyclonedx.json") String ossResults
    @Shared @AutoCleanup FoDWebAppSupplier app = new FoDWebAppSupplier()

    def "get.byId"() {
        def args = "fod sast-scan get ::scans::get(0).scanId"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[1].startsWith("startedByUserId: ")
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

