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

@Prefix("ssc.job") @FcliSession(SSC) @Stepwise
class SSCJobSpec extends FcliBaseSpec {
    @Shared @AutoCleanup SSCAppVersion version = new SSCAppVersion().create()
    @Shared @TestResource("runtime/shared/EightBall-22.1.0.fpr") String fpr
    @Shared String uploadVariableName = version.fcliVariableName+"_artifact"
    @Shared String uploadVariableRef = "::$uploadVariableName::"

    //these uploads are here to create cancellable jobs
    def "upload"() {
        def args = "ssc artifact upload $fpr --appversion "+ 
            "${version.variableRef} --store $uploadVariableName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it[0] =~ /Id\s+Scan types\s+Last scan date\s+Upload date\s+Status/
                it[1] =~ /^\s*\d+.*/
            }
        where:
            i << (1..5)
    }
    
    def "list"() {
        def args = "ssc job list --store jobs -q cancellable==true"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("JobnameJobgroupJobclassStateCancellablePriorityCreatetimeStarttimeFinishtime")
                it.any { it.startsWith(" JOB") }
            }
    }
    
    def "update"() {
<<<<<<< HEAD
        def args = "ssc job update ::jobs::get(0).jobName --priority 1 --store job"
=======
        def args = "ssc job update ::jobs::get(#var('jobs').size()-1).jobName --priority 1"
>>>>>>> parent of f03f75c71 (chore: updated tests, added spock annotation extension)
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
            }
    }

    def "cancel"() {
        def args = "ssc job cancel ::jobs::get(0).jobName"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[1].contains("CANCEL_REQUESTED")
            }
    }

    def "get.byName"() {
<<<<<<< HEAD
        Thread.sleep(1000)
        def args = "ssc job get ::job::jobName"
=======
        def args = "ssc job get ::jobs::get(0).jobName"
>>>>>>> parent of f03f75c71 (chore: updated tests, added spock annotation extension)
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.any { it.startsWith("priority: 1") }
                it.any { it.equals("cancelRequested: true") || it.equals("state: \"CANCELLED\"")}
            }
    }
}
