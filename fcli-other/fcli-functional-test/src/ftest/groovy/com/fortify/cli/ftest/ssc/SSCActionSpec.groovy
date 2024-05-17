/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.ssc

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempFile
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise
import java.nio.file.Files
import java.nio.file.Path
import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

@Prefix("ssc.action") @FcliSession(SSC) @Stepwise
class SSCActionSpec extends FcliBaseSpec {
    @Shared @TempFile("bb-fortify-report.json") String bitBucketFortifyReportFile
    @Shared @TempFile("bb-fortify-annotations.json") String bitBucketFortifyAnnotationsFile
    @Shared @AutoCleanup SSCAppVersionSupplier version1Supplier = new SSCAppVersionSupplier()
    @Shared @AutoCleanup SSCAppVersionSupplier version2Supplier = new SSCAppVersionSupplier()
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String fpr1
    @Shared @TestResource("runtime/shared/LoginProject.fpr") String fpr2

    def setupSpec() {
        Fcli.run("ssc artifact upload -f $fpr1 --appversion ${version1Supplier.version.variableRef} --store fpr1")
        Fcli.run("ssc artifact upload -f $fpr2 --appversion ${version2Supplier.version.variableRef} --store fpr2")
        Fcli.run("ssc artifact wait-for ::fpr1:: ::fpr2:: -i 2s")
    }

    def "createBitBucketSastReport"(){
        def args = "ssc action run bitbucket-sast-report --av ${version1Supplier.version.get("id")} -r ${{bitBucketFortifyReportFile}} -a ${{bitBucketFortifyAnnotationsFile}}"
        when:
        def result = Fcli.run(args)
        then:
        verifyAll(result.stdout) {
            size() == 2
            Files.exists(Path.of(bitBucketFortifyReportFile))
            Files.exists(Path.of(bitBucketFortifyAnnotationsFile))
        }
    }
}
