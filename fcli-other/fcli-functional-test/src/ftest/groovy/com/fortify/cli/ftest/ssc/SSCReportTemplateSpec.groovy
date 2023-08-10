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

@Prefix("ssc.report-template") @FcliSession(SSC) @Stepwise
class SSCReportTemplateSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/ssc/project_report.rptdesign") String sampleTemplate
    @Shared @TestResource("runtime/ssc/ReportTemplateConfig.yml") String sampleConfig
    private String reportName = "fcli-test-report-"+System.currentTimeMillis()
    
    def "list"() {
        def args = "ssc report-template list --store reports"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameTypeTemplatedocidInuse")
            }
    }    
    
    def "create"() {
        def args = "ssc report-template create --template $sampleTemplate --config $sampleConfig"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("CREATED")
            }
    }
    
    def "generate-config"() {
        def args = "ssc report-template generate-config -y"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("GENERATED")
            }
    }
    
    def "get.byName"() {
        def args = "ssc report-template get $reportName --store report"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[2].equals("name: \"fcli-test-report\"")
            }
    }
    
    def "get.byId"() {
        def args = "ssc report-template get ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[2].equals("name: \"fcli-test-report\"")
            }
    }
    
    def "download"() {
        def args = "ssc report-template download ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DOWNLOADED")
            }
    }
    
    def "delete"() {
        def args = "ssc report-template delete ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DELETED")
            }
    }
    
    def "verifyDeleted"() {
            def args = "ssc report-template list"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    !it.any { it.contains(reportName) }
                }
    }
    
}
