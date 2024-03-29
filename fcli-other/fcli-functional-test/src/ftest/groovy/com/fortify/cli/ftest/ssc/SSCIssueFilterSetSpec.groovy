package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion-filterset") @FcliSession(SSC) @Stepwise
class SSCIssueFilterSetSpec extends FcliBaseSpec {
    @Global(SSCAppVersionSupplier.Empty.class) SSCAppVersionSupplier versionSupplier;
    
    def "list"() {
        def args = "ssc issue list-filtersets --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName + " --store filtersets"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(" ","").equals("GuidTitleDefaultfilterset");
                it.any { it.contains("Security Auditor View") }
            }
    }
    
    def "get.byId"() {
        def args = "ssc issue get-filterset ::filtersets::get(0).guid --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("title: \"Security Auditor View\"")
            }
    }
    
    def "get.byTitle"() {
        def args = "ssc issue get-filterset Security\\ Auditor\\ View --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName 
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>2
                it[2].equals("title: \"Security Auditor View\"")
            }
    }
}