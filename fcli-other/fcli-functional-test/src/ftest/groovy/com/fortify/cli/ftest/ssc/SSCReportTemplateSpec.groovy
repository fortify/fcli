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

import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.report-template") @FcliSession(SSC) @Stepwise
class SSCReportTemplateSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/ssc/project_report.rptdesign") String sampleTemplate
    @Shared @TestResource("runtime/ssc/ReportTemplateConfig.yml") String sampleConfig
    private String reportName = "fcli-test-report"
    
    def "list"() {
        def args = "ssc report list-templates --store reports"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameTypeTemplatedocidInuse")
            }
    }    
    
    def "create"() {
        def args = "ssc report create-template --template $sampleTemplate --config $sampleConfig"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("CREATED")
            }
    }
    
    def "generate-config"() {
        def args = "ssc report create-template-config -y"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[1].contains("GENERATED")
            }
    }
    
    def "get.byName"() {
        def args = "ssc report get-template $reportName --store report"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[2].equals("name: \"fcli-test-report\"")
            }
    }
    
    def "get.byId"() {
        def args = "ssc report get-template ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it[2].equals("name: \"fcli-test-report\"")
            }
    }
    
    def "download"() {
        def args = "ssc report download-template ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DOWNLOADED")
            }
    }
    
    def "delete"() {
        def args = "ssc report delete-template ::report::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DELETED")
            }
    }
    
    def "verifyDeleted"() {
            def args = "ssc report list-templates"
            when:
                def result = Fcli.run(args)
            then:
                verifyAll(result.stdout) {
                    size()>0
                    !it.any { it.contains(reportName) }
                }
    }
    
}
