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

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.SSC

import java.nio.file.Files
import java.nio.file.Path

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.Shared

@Prefix("ssc.action.run") @FcliSession(SSC)
class SSCBuiltinActionRunSpec extends FcliBaseSpec {
    @Shared @TempDir("action-output") String actionOutputDir;
    @Global(SSCAppVersionSupplier.EightBall.class) SSCAppVersionSupplier eightBallVersionSupplier;
    
    def "runWithOutputFile"() {
        def random = System.currentTimeMillis()
        def outputFile = "${actionOutputDir}/output-${random}"
        def args = "ssc action run ${action} -f ${outputFile} --av ${eightBallVersionSupplier.version.get("id")}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==1
                Files.exists(Path.of(outputFile))
            }
        where:
            action << ['appversion-summary',
                       'github-sast-report',
                       'gitlab-dast-report',
                       'gitlab-debricked-report',
                       'gitlab-sast-report',
                       'gitlab-sonatype-report',
                       'sarif-sast-report',
                       'sonarqube-sast-report']
    }
    
    def "runBitBucketSastReport"() {
        def random = System.currentTimeMillis()
        def reportFile = "${actionOutputDir}/bb-report-${random}"
        def annotationsFile = "${actionOutputDir}/bb-annotations-${random}"
        def args = "ssc action run bitbucket-sast-report -r ${reportFile} -a ${annotationsFile} --av ${eightBallVersionSupplier.version.get("id")}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                Files.exists(Path.of(reportFile))
                Files.exists(Path.of(annotationsFile))
            }
    }
    
    def "runCheckPolicy"() {
        def args = "ssc action run check-policy --av ${eightBallVersionSupplier.version.get("id")}"
        when:
            def result = Fcli.run(args, {})
        then:
            verifyAll(result.stdout) {
                size()>1
                it.any { it.contains('PASS') || it.contains('FAIL') }
                it.any { it.contains("Status: ") }
            }
    }
}
