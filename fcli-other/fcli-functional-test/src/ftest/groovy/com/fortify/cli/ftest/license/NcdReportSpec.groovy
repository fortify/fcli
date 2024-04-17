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
package com.fortify.cli.ftest.license

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TempFile
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Requires
import spock.lang.Shared

@Prefix("report.ncd-license")
class NcdReportSpec extends FcliBaseSpec {
    @Shared @TempFile("ncd-report-config/sample.yaml") String sampleConfigOutputFile;
    @Shared @TestResource("runtime/report/ncd-report.yml") String configFile;
    @Shared @TempDir("ncd-report") String reportOutputDir;
    @Shared @TempFile("ncd-report.zip") String reportOutputZip;
    
    def "generate-config"() {
        def args = "license ncd-report create-config -y -c ${sampleConfigOutputFile} -o yaml"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) { 
                size() == 3
                it[1] ==~ /^path: $sampleConfigOutputFile$/
                it[2] ==~ /^__action__: GENERATED$/
            }
            new File(sampleConfigOutputFile).exists()
    }
    
    @Requires({env.FCLI_FT_GITHUB_TOKEN && env.FCLI_FT_GITLAB_TOKEN})
    def "generate-dir"() {
        def args = "license ncd-report create -y -c ${configFile} -d ${reportOutputDir}"
        when:
            def result = Fcli.run(args)
        then:
            new File("${reportOutputDir}/summary.txt").exists()
            new File("${reportOutputDir}/contributors.csv").exists()
            new File("${reportOutputDir}/report-config.yaml").exists()
            new File("${reportOutputDir}/report.log").exists()
            new File("${reportOutputDir}/checksums.sha256").exists()
            new File("${reportOutputDir}/details/commits-by-branch.csv").exists()
            new File("${reportOutputDir}/details/commits-by-repository.csv").exists()
            new File("${reportOutputDir}/details/contributors-by-repository.csv").exists()
            new File("${reportOutputDir}/details/repositories.csv").exists()
            verifyAll(result.stdout) {
                it.any { it == "reportPath: ${reportOutputDir}" }
                it.any { it == '  reportType: Number of Contributing Developers (NCD) Report' }
                it.any { it.contains("repositoryCounts:") }
                it.any { it.contains("commitCount:") }
                it.any { it.contains("authorCount:") }
                it.any { it.contains("logCounts:") }
            }
    }
    
    @Requires({env.FCLI_FT_GITHUB_TOKEN && env.FCLI_FT_GITLAB_TOKEN})
    def "generate-zip"() {
        def args = "license ncd-report create -y -c ${configFile} -z ${reportOutputZip}"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                it.any { it == "reportPath: ${reportOutputZip}" }
                it.any { it == '  reportType: Number of Contributing Developers (NCD) Report' }
                it.any { it.contains("repositoryCounts:") }
                it.any { it.contains("commitCount:") }
                it.any { it.contains("authorCount:") }
                it.any { it.contains("logCounts:") }
            }
    }
}
