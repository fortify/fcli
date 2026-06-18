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

    private String tempPath(String relativePath) {
        return new File(mockReportDir, relativePath).absolutePath
    }
    
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
    
    // ===== Mock Source Tests =====
    
    @Shared @TestResource("runtime/report/ncd-report-mock.yml") String mockConfigFile;
    @Shared @TempDir("ncd-report-mock") String mockReportDir;
    @Shared @TempFile("ncd-report-mock.zip") String mockReportZip;
    
    def "mock-generate-dir"() {
        def args = "license ncd-report create -y -c ${mockConfigFile} -d ${mockReportDir}"
        when:
            def result = Fcli.run(args)
        then:
            new File("${mockReportDir}/summary.txt").exists()
            new File("${mockReportDir}/contributors.csv").exists()
            new File("${mockReportDir}/checksums.sha256").exists()
            verifyAll(result.stdout) {
                it.any { it == "reportPath: ${mockReportDir}" }
                it.any { it == '  reportType: Number of Contributing Developers (NCD) Report' }
                it.any { it.contains("authorCount:") }
                it.any { it.contains("commitCount:") }
            }
    }
    
    def "mock-generate-with-end-date"() {
        def args = "license ncd-report create -y -c ${mockConfigFile} -d ${mockReportDir}-enddate --end-date 2026-06-01"
        when:
            def result = Fcli.run(args)
        then:
            new File("${mockReportDir}-enddate/summary.txt").exists()
            verifyAll(result.stdout) {
                it.any { it == "reportPath: ${mockReportDir}-enddate" }
                it.any { it.contains("authorCount:") }
            }
    }
    
    def "mock-list-contributors"() {
        def reportDir = tempPath("ncd-report-list-contributors")
        def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}"
        def listArgs = "license ncd-report list-contributors -r ${reportDir}"
        when:
            Fcli.run(createArgs)
            def result = Fcli.run(listArgs)
        then:
            result.stdout.size() > 0
            result.stdout.any { it.contains("Author name") || it.contains("Author email") }
    }
    
    def "mock-list-contributors-json"() {
        def reportDir = tempPath("ncd-report-list-contributors-json")
        def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}"
        def listArgs = "license ncd-report list-contributors -r ${reportDir} -o json"
        when:
            Fcli.run(createArgs)
            def result = Fcli.run(listArgs)
        then:
            result.stdout.any { it.contains("authorId") }
            result.stdout.any { it.contains("authorName") }
            result.stdout.any { it.contains("contributionStatus") }
    }
    
    def "mock-list-contributors-csv"() {
        def reportDir = tempPath("ncd-report-list-contributors-csv")
        def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}"
        def listArgs = "license ncd-report list-contributors -r ${reportDir} -o csv"
        when:
            Fcli.run(createArgs)
            def result = Fcli.run(listArgs)
        then:
            result.stdout[0].contains("authorId")
            result.stdout[0].contains("authorName")
            result.stdout[0].contains("contributionStatus")
            result.stdout.size() > 2  // Header + at least one author
    }
    
    def "mock-merge"() {
        def config1 = tempPath("ncd-report-mock-1.yml")
        def config2 = tempPath("ncd-report-mock-2.yml")
        def report1 = tempPath("ncd-report-mock-1")
        def report2 = tempPath("ncd-report-mock-2")
        def mergedReport = tempPath("ncd-report-merged")
        
        when:
            // Create two separate reports to merge
            def createArgs1 = "license ncd-report create -y -c ${mockConfigFile} -d ${report1}"
            def result1 = Fcli.run(createArgs1)
            
            def createArgs2 = "license ncd-report create -y -c ${mockConfigFile} -d ${report2}"
            def result2 = Fcli.run(createArgs2)
            
            // Merge the two reports
            def mergeArgs = "license ncd-report merge -r ${report1} ${report2} -d ${mergedReport} -y"
            def mergeResult = Fcli.run(mergeArgs)
        then:
            new File("${mergedReport}/summary.txt").exists()
            new File("${mergedReport}/contributors.csv").exists()
            mergeResult.stdout.any { it.contains("mergedReportCount: 2") }
    }
    
    def "mock-update-from-list-output"() {
        def report1 = tempPath("ncd-report-update-source")
        def tmpListOutput = tempPath("ncd-contributors-list.csv")
        
        when:
            // Create a report for list
            def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${report1}"
            Fcli.run(createArgs)
            
            // List contributors to CSV
            def listArgs = "license ncd-report list-contributors -r ${report1} -o csv --to-file ${tmpListOutput}"
            Fcli.run(listArgs)
            
            // Update the same report with the list output
            def updateArgs = "license ncd-report update -r ${report1} -c ${tmpListOutput}"
            Fcli.run(updateArgs)
        then:
            new File(tmpListOutput).exists()
    }
    
    def "mock-list-contributors-realistic-names"() {
        def reportDir = tempPath("ncd-report-realistic-names")
        def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}"
        def listArgs = "license ncd-report list-contributors -r ${reportDir} -o table"
        when:
            Fcli.run(createArgs)
            def result = Fcli.run(listArgs)
        then:
            // Should contain realistic author names like "John Smith", "Sarah Johnson", etc.
            result.stdout.any { it.contains("Smith") || it.contains("Johnson") || it.contains("Chen") || it.contains("Williams") }
    }
    
    def "mock-detect-duplicates"() {
        def duplicateReportDir = tempPath("ncd-report-duplicates")
        def tmpListOutput = tempPath("duplicates-list.json")
        
        when:
            // Create report with realistic data (which includes duplicates)
            def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${duplicateReportDir}"
            Fcli.run(createArgs)
            
            // List all contributors to see duplicates
            def listArgs = "license ncd-report list-contributors -r ${duplicateReportDir} -o json --to-file ${tmpListOutput}"
            Fcli.run(listArgs)
        then:
            new File(tmpListOutput).exists()
    }
    
    def "mock-update-ai-duplicates"() {
        def reportPath = tempPath("ncd-report-ai-duplicates")
        def updateData = tempPath("ai-duplicates.json")
        
        when:
            // Create report
            def createArgs = "license ncd-report create -y -c ${mockConfigFile} -d ${reportPath}"
            Fcli.run(createArgs)

                        def contributorLines = new File("${reportPath}/contributors.csv").readLines().drop(1)
                        def authorIds = contributorLines.collect { it.split(',', -1)[0] }.findAll { it }.unique()
            
            // Create update data with AI-detected duplicates
            // Format: authorId pairs where AI thinks they're the same person
            new File(updateData).text = '''[
  {
        "authorId": "''' + authorIds[0] + '''",
        "aiDuplicateOf": "''' + authorIds[1] + '''",
    "aiConfidence": "0.95"
  },
  {
        "authorId": "''' + authorIds[2] + '''",
        "aiDuplicateOf": "''' + authorIds[3] + '''",
    "aiConfidence": "0.85"
  }
]'''
            
            // Try to update (may not find exact matches in generated data, but validates the command)
                        def updateArgs = "license ncd-report update -r ${reportPath} -c ${updateData}"
            Fcli.run(updateArgs)
        then:
            new File(reportPath).exists()
    }
    
    def "mock-datafile-json"() {
        def reportDir = tempPath("ncd-report-with-json-data")
        def configYaml = tempPath("ncd-report-json-data.yml")
        def mockDataFile = "src/ftest/resources/runtime/report/mock-authors.json"
        
        when:
            // Create config that references JSON data file
            new File(configYaml).text = """
contributor:
  ignoreExpression: >
    lcName matches '.*\\[bot\\]'
  duplicateExpression: >
    a1.cleanName==a2.cleanName ||
    a1.cleanEmailName==a2.cleanEmailName ||
    a1.cleanName==a2.cleanEmailName

sources:
  mock:
    - repositoryCount: 1
      authorsPerRepository: 2
      commitsPerAuthor: 3
      dataFile: ${mockDataFile}
"""
            
            def createArgs = "license ncd-report create -y -c ${configYaml} -d ${reportDir}"
            def result = Fcli.run(createArgs)
        then:
            new File("${reportDir}/summary.txt").exists()
            new File("${reportDir}/contributors.csv").exists()
            result.stdout.any { it.contains("reportPath") }
    }
    
    def "mock-datafile-csv"() {
        def reportDir = tempPath("ncd-report-with-csv-data")
        def configYaml = tempPath("ncd-report-csv-data.yml")
        def mockDataFile = "src/ftest/resources/runtime/report/mock-authors.csv"
        
        when:
            // Create config that references CSV data file
            new File(configYaml).text = """
contributor:
  ignoreExpression: >
    lcName matches '.*\\[bot\\]'
  duplicateExpression: >
    a1.cleanName==a2.cleanName ||
    a1.cleanEmailName==a2.cleanEmailName ||
    a1.cleanName==a2.cleanEmailName

sources:
  mock:
    - repositoryCount: 1
      authorsPerRepository: 2
      commitsPerAuthor: 3
      dataFile: ${mockDataFile}
"""
            
            def createArgs = "license ncd-report create -y -c ${configYaml} -d ${reportDir}"
            def result = Fcli.run(createArgs)
        then:
            new File("${reportDir}/summary.txt").exists()
            new File("${reportDir}/contributors.csv").exists()
            result.stdout.any { it.contains("reportPath") }
    }
}
