package com.fortify.cli.ftest.ssc;

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.appversion-attribute") @FcliSession(SSC) @Stepwise
class SSCAttributeSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersionSupplier versionSupplier = new SSCAppVersionSupplier()
    
    def "list"() {
        def args = "ssc attribute list --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(" ","").equals("IdCategoryGuidNameValue");
            }
    }
    
    def "set"() {
        def args = "ssc attribute update --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName +
                    " --attrs BusinessRisk=High,BusinessUnit=Corporate"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==3
                it[0].replace(" ","").equals("IdCategoryGuidNameValue");
            }
    }
    
    def "verify"() {
        def args = "ssc attribute list --appversion " + versionSupplier.version.appName + ":" + versionSupplier.version.versionName
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("BUSINESS") && it.contains("BusinessRisk") && it.contains("High") }
                it.any { it.contains("ORGANIZATION") && it.contains("BusinessUnit") && it.contains("Corporate") }
            }
    }
}