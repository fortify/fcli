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
import com.fortify.cli.ftest.ssc._common.SSCAppVersion

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

@Prefix("ssc.job") @FcliSession(SSC) 
class SSCJobSpec extends FcliBaseSpec {
    
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
    
    def "extractJobName"() {
        def args = "util variable contents jobs -q jobName==#var('jobs').get(0).jobName --store job"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[0].replace(' ', '').equals("JobnameJobgroupJobclassUsernameProjectversionidStateExecutionorderPriorityStarttimeFinishtimeJobdataauthenticationusernameJobdataparamprojectversionnameJobdatajobuuidJobdataparamprojectnameJobdataparamprojectversionidCancellableCancelrequestedCreatetimeHref")
                
            }
    }
    
    //TODO replace hardcoded value
    def "get.byName"() {
        def args = "ssc job get ReportCleanup\$743d0bdc-4570-4b7b-a5d5-c4373f9d7732"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.any { it.startsWith("jobData:") }
            }
    }
    
    //TODO add tests for cancel + update
}
