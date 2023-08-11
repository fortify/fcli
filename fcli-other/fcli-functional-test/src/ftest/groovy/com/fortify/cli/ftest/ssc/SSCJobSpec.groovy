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

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll
import com.fortify.cli.ftest._common.StepwiseExcept

/*The @StepwiseExcept annotation is used here to allow some of the upload iterations to fail 
 *and continue test execution regardless. To achieve this a test identifier in the format
 *<prefix> (Classname).featurename is passed to declare an exception for the tests in question
*/ 
@Prefix("ssc.job") @FcliSession(SSC) @StepwiseExcept(except="ssc.job (SSCJobSpec).upload")
class SSCJobSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String fpr
    @Shared @TestResource("runtime/shared/LoginProject.fpr") String diffpr
    @Shared String uploadVariableName = version.fcliVariableName+"_artifact"
    @Shared String uploadVariableRef = "::$uploadVariableName::"
    @Shared int repeats = 5;

    /*
     * these uploads are here to create cancellable jobs
     * the most reliable way seems to be to upload differing artifacts
     * and then approve the last one uploaded, so multiple processing jobs are scheduled.
     * this assumes that the processing rule with identifier
     * "com.fortify.manager.BLL.processingrules.VetoCascadingApprovalProcessingRule"
     * is NOT enabled on the application version in question
     
    @Unroll
    def "upload"() {
        def args = "ssc artifact upload $fpr --appversion "+ 
            "${version.variableRef} --store $uploadVariableName"
        when:
            def result = Fcli.run(args)
            fpr = diffpr
            if(i==repeats) {
            Fcli.run("ssc artifact wait-for $uploadVariableRef -i 1s -s PROCESS_COMPLETE,REQUIRE_AUTH")
            Fcli.run("ssc artifact approve ::$uploadVariableName::id")
            }
        then:
            verifyAll(result.stdout) {
                it[0] =~ /Id\s+Scan types\s+Last scan date\s+Upload date\s+Status/
                it[1] =~ /^\s*\d+./
            }
        where:
            i << (1..repeats)
    }
    
    def "update"() {
        def args = "ssc job update ::jobs::get(0).jobName --priority 1 --store job"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "cancel"() {
        def args = "ssc job cancel ::job::jobName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("CANCEL_REQUESTED")
            }
    }
    */
    def "list"() {
        def args = "ssc job list --store jobs"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("JobnameJobgroupJobclassStateCancellablePriorityCreatetimeStarttimeFinishtime")
                it.any { it.startsWith(" JOB") }
            }
    }

    def "get.byName"() {

        Thread.sleep(1000)
        def args = "ssc job get ::jobs::get(0).jobName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[1].startsWith("jobName:")
            }
    }
}
