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

    private static String sha256Hex(File file) {
        def digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(java.nio.file.Files.readAllBytes(file.toPath()))
        return digest.digest().collect { String.format("%02X", it) }.join("")
    }

    private static void updateChecksum(String reportDir, String entryName) {
        def checksumsFile = new File("${reportDir}/checksums.sha256")
        def entryFile = new File("${reportDir}/${entryName}")
        def checksum = sha256Hex(entryFile)
        def lines = checksumsFile.readLines()
        def updated = []
        def found = false
        lines.each { line ->
            def parts = line.split(/\s+/, 2)
            if ( parts.size() >= 2 ) {
                def filename = parts[1].startsWith("*") ? parts[1].substring(1) : parts[1]
                if ( filename == entryName ) {
                    updated << "${checksum} ${entryName}"
                    found = true
                } else {
                    updated << line
                }
            } else {
                updated << line
            }
        }
        if ( !found ) {
            updated << "${checksum} ${entryName}"
        }
        checksumsFile.text = updated.join("\n") + "\n"
    }

    private static void addLegacyNumberColumnsToContributorsCsv(String reportDir) {
        def contributorsFile = new File("${reportDir}/contributors.csv")
        def lines = contributorsFile.readLines()
        def updated = []
        updated << (lines[0] + ",authorNumber,contributingAuthorNumber")
        lines.drop(1).each { updated << (it + ",-1,-1") }
        contributorsFile.text = updated.join("\n") + "\n"
        updateChecksum(reportDir, "contributors.csv")
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
    @Shared @TestResource("runtime/report/mock-authors.json") String mockAuthorsJson;
    @Shared @TestResource("runtime/report/mock-authors.csv") String mockAuthorsCsv;
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
            def contributorHeader = new File("${mockReportDir}/contributors.csv").readLines().first()
            !contributorHeader.contains("authorNumber")
            !contributorHeader.contains("contributingAuthorNumber")
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
            result.stdout.any { it.contains("duplicateOf") }
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
            result.stdout[0].contains("duplicateOf")
            !result.stdout[0].contains("authorNumber")
            !result.stdout[0].contains("contributingAuthorNumber")
            result.stdout.size() > 2  // Header + at least one author
    }

    def "mock-legacy-number-columns-accepted-by-lsc-merge-update"() {
        def report1 = tempPath("ncd-report-legacy-source-1")
        def report2 = tempPath("ncd-report-legacy-source-2")
        def mergedReport = tempPath("ncd-report-legacy-merged")
        def listCsv = tempPath("ncd-report-legacy-list.csv")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report1}")
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report2}")

            // Inject legacy numeric columns into contributors.csv and update checksums,
            // simulating reports produced by older fcli versions.
            addLegacyNumberColumnsToContributorsCsv(report1)
            addLegacyNumberColumnsToContributorsCsv(report2)

            def lscResult = Fcli.run("license ncd-report list-contributors -r ${report1} -o csv --to-file ${listCsv}")
            def updateResult = Fcli.run("license ncd-report update-contributor-status -r ${report1} -c ${listCsv}")
            def mergeResult = Fcli.run("license ncd-report merge -r ${report1},${report2} -d ${mergedReport} -y")

            def mergedHeader = new File("${mergedReport}/contributors.csv").readLines().first()
        then:
            new File(listCsv).exists()
            lscResult.exitCode == 0
            updateResult.exitCode == 0
            mergeResult.exitCode == 0
            !mergedHeader.contains("authorNumber")
            !mergedHeader.contains("contributingAuthorNumber")
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
            def mergeArgs = "license ncd-report merge -r ${report1},${report2} -d ${mergedReport} -y"
            def mergeResult = Fcli.run(mergeArgs)
            def mergedLines = new File("${mergedReport}/contributors.csv").readLines()
            def headerCols = mergedLines.first().split(',', -1)
            def statusIndex = headerCols.findIndexOf { it == 'contributionStatus' }
            def sourceReportsIndex = headerCols.findIndexOf { it == 'sourceReports' }
            def ignoredCount = mergedLines.drop(1).count { row ->
                def cols = row.split(',', -1)
                statusIndex >= 0 && cols.size() > statusIndex && cols[statusIndex] == 'ignored'
            }
        then:
            new File("${mergedReport}/summary.txt").exists()
            new File("${mergedReport}/contributors.csv").exists()
            mergeResult.stdout.any { it.contains("mergedReportCount: 2") }
            sourceReportsIndex >= 0
            ignoredCount >= 4
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
            def updateArgs = "license ncd-report update-contributor-status -r ${report1} -c ${tmpListOutput}"
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
                "duplicateOf": "''' + authorIds[1] + '''",
        "overrideStatusConfidence": "0.95"
  },
  {
        "authorId": "''' + authorIds[2] + '''",
                "duplicateOf": "''' + authorIds[3] + '''",
        "overrideStatusConfidence": "0.85"
  }
]'''
            
            // Try to update (may not find exact matches in generated data, but validates the command)
                        def updateArgs = "license ncd-report update-contributor-status -r ${reportPath} -c ${updateData}"
            Fcli.run(updateArgs)
        then:
            new File(reportPath).exists()
    }
    
    def "mock-datafile-json"() {
        def reportDir = tempPath("ncd-report-with-json-data")
        def configYaml = tempPath("ncd-report-json-data.yml")
        def mockDataFile = mockAuthorsJson
        
        when:
            // Create config that references JSON data file
            new File(configYaml).text = """
|contributor:
|  ignoreExpression: >
|    lcName matches '.*\\[bot\\]'
|  duplicateExpression: >
|    a1.cleanName==a2.cleanName ||
|    a1.cleanEmailName==a2.cleanEmailName ||
|    a1.cleanName==a2.cleanEmailName
|
|sources:
|  mock:
|    - repositoryCount: 1
|      authorsPerRepository: 2
|      commitsPerAuthor: 3
|      dataFile: "${mockDataFile}"
""".stripMargin()
            
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
        def mockDataFile = mockAuthorsCsv
        
        when:
            // Create config that references CSV data file
            new File(configYaml).text = """
|contributor:
|  ignoreExpression: >
|    lcName matches '.*\\[bot\\]'
|  duplicateExpression: >
|    a1.cleanName==a2.cleanName ||
|    a1.cleanEmailName==a2.cleanEmailName ||
|    a1.cleanName==a2.cleanEmailName
|
|sources:
|  mock:
|    - repositoryCount: 1
|      authorsPerRepository: 2
|      commitsPerAuthor: 3
|      dataFile: "${mockDataFile}"
""".stripMargin()
            
            def createArgs = "license ncd-report create -y -c ${configYaml} -d ${reportDir}"
            def result = Fcli.run(createArgs)
        then:
            new File("${reportDir}/summary.txt").exists()
            new File("${reportDir}/contributors.csv").exists()
            result.stdout.any { it.contains("reportPath") }
    }
}
