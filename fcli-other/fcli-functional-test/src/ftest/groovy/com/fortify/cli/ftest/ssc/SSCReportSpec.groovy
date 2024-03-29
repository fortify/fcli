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
import java.text.SimpleDateFormat

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Global
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TempFile
import com.fortify.cli.ftest._common.spec.TestResource
import com.fortify.cli.ftest.ssc._common.SSCAppVersionSupplier

import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("ssc.report") @FcliSession(SSC) @Stepwise
class SSCReportSpec extends FcliBaseSpec {
    @Shared @TempFile("report.pdf") String reportFile;
    @Global(SSCAppVersionSupplier.EightBall.class) SSCAppVersionSupplier eightBallVersionSupplier;
    @Global(SSCAppVersionSupplier.LoginProject.class) SSCAppVersionSupplier loginProjectVersionSupplier;
    @Shared String random = System.currentTimeMillis()
    @Shared String owaspReportName = "fcli-OWASP-${random}"
    @Shared String trendingReportName = "fcli-Trending-${random}"
    @Shared String kpiReportName = "fcli-KPI-${random}"
    
    def "createOWASP"() {
        def args = "ssc report create --template OWASP\\ Top\\ 10 --name ${owaspReportName} -p Application\\ Version=${eightBallVersionSupplier.version.get("id")} --store owasp"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("CREATED")
            }
    }
    
    def "createTrending"() {
        def today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        def args = "ssc report create --template Issue\\ Trending --name ${trendingReportName} -p startDate=2010-01-01,endDate=${today},projectversionids=[${eightBallVersionSupplier.version.get("id")};${loginProjectVersionSupplier.version.get("id")}] --store trending"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("CREATED")
            }
    }
    
    def "createKPI"() {
        def args = "ssc report create --template Key\\ Performance\\ Indicators --name ${kpiReportName} -p Application\\ Attribute=Accessibility,projectversionids=[${eightBallVersionSupplier.version.get("id")};${loginProjectVersionSupplier.version.get("id")}] --store kpi"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.last().contains("CREATED")
            }
    }
    
    def "wait-for"() {
        // Try waiting both by default property (id), and name
        def args = "ssc report wait-for ::owasp:: ::trending::name ::kpi:: -i 2s"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it =~ "WAIT_COMPLETE" }
            }
    }
    
    def "list"() {
        def args = "ssc report list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("IdNameTemplatenameTypeFinishdateGeneratedbyStatus")
                it.any { it.contains(owaspReportName) }
                it.any { it.contains(trendingReportName) }
                it.any { it.contains(kpiReportName) }
            }
    }    
    
    def "get.byName"() {
        def args = "ssc report get ::owasp::name"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.any { it.contains(owaspReportName) }
            }
    }
    
    def "get.byId"() {
        def args = "ssc report get ::trending::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>=2
                it.any { it.contains(trendingReportName) }
            }
    }
    
    def "download"() {
        def args = "ssc report download ::owasp::id -f ${reportFile}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DOWNLOADED")
                Files.exists(Path.of(reportFile))
            }
    }
    
    def "deleteOWASP"() {
        def args = "ssc report delete ::owasp::id"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DELETED")
            }
    }
    
    def "deleteTrending"() {
        def args = "ssc report delete ::trending::name"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it.last().contains("DELETED")
            }
    }
    
    def "deleteKPI"() {
        def args = "ssc report delete ::kpi::"
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
                    !it.any { it.contains(owaspReportName) }
                    !it.any { it.contains(trendingReportName) }
                    !it.any { it.contains(kpiReportName) }
                }
    }
}
