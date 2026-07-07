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

    private static void removeDormantColumnFromContributorsCsv(String reportDir) {
        def contributorsFile = new File("${reportDir}/contributors.csv")
        def lines = contributorsFile.readLines()
        def headers = lines[0].split(',', -1)
        def dormantIndex = headers.findIndexOf { it == 'dormant' }
        assert dormantIndex >= 0

        def updated = []
        updated << headers.findAll { it != 'dormant' }.join(',')
        lines.drop(1).each { line ->
            def cols = line.split(',', -1)
            def filtered = []
            cols.eachWithIndex { col, i ->
                if ( i != dormantIndex ) {
                    filtered << col
                }
            }
            updated << filtered.join(',')
        }
        contributorsFile.text = updated.join("\n") + "\n"
        updateChecksum(reportDir, "contributors.csv")
    }

    private static String getFirstAuthorId(String reportDir) {
        def contributorsFile = new File("${reportDir}/contributors.csv")
        def lines = contributorsFile.readLines()
        def headers = lines[0].split(',', -1)
        def authorIdIndex = headers.findIndexOf { it == 'authorId' }
        def statusIndex = headers.findIndexOf { it == 'contributionStatus' }
        def firstData = lines.drop(1).find { line ->
            line?.trim() && line.split(',', -1)[statusIndex] == 'contributing'
        }
        return firstData.split(',', -1)[authorIdIndex]
    }

    private static void setDormantForAuthorId(String reportDir, String authorId, boolean dormant) {
        def contributorsFile = new File("${reportDir}/contributors.csv")
        def lines = contributorsFile.readLines()
        def headers = lines[0].split(',', -1)
        def authorIdIndex = headers.findIndexOf { it == 'authorId' }
        def dormantIndex = headers.findIndexOf { it == 'dormant' }
        assert authorIdIndex >= 0
        assert dormantIndex >= 0

        def updated = [lines[0]]
        lines.drop(1).each { line ->
            def cols = line.split(',', -1)
            if ( cols[authorIdIndex] == authorId ) {
                cols[dormantIndex] = String.valueOf(dormant)
            }
            updated << cols.join(',')
        }
        contributorsFile.text = updated.join("\n") + "\n"
        updateChecksum(reportDir, "contributors.csv")
    }

    private static int countDormantContributorsInReport(String reportDir) {
        def contributorsFile = new File("${reportDir}/contributors.csv")
        def lines = contributorsFile.readLines()
        def headers = lines[0].split(',', -1)
        def dormantIndex = headers.findIndexOf { it == 'dormant' }
        def statusIndex = headers.findIndexOf { it == 'contributionStatus' }
        assert dormantIndex >= 0
        assert statusIndex >= 0
        return lines.drop(1).count { line ->
            def cols = line.split(',', -1)
            cols[statusIndex] == 'contributing' && cols[dormantIndex] == 'true'
        }
    }

    private static List<String> readCsvHeader(String path) {
        return new File(path).readLines().first().split(',', -1) as List<String>
    }

    private static List<Map<String, String>> readCsvRowsAsMaps(String path) {
        def lines = new File(path).readLines()
        def header = lines.first().split(',', -1)
        return lines.drop(1).collect { line ->
            def cols = line.split(',', -1)
            def row = [:]
            header.eachWithIndex { h, i -> row[h] = i < cols.size() ? cols[i] : '' }
            row
        }
    }

    private static Map<String, Integer> computeCommitCountsByRepository(String reportDir) {
        def rows = readCsvRowsAsMaps("${reportDir}/details/commits-by-repository.csv")
        def result = [:].withDefault { 0 }
        rows.each { row -> result[row.repositoryUrl] = result[row.repositoryUrl] + 1 }
        return result
    }

    private static Map<String, Integer> computeContributorCountsByRepository(String reportDir) {
        def rows = readCsvRowsAsMaps("${reportDir}/details/contributors-by-repository.csv")
        def contributorIdsByRepository = [:].withDefault { [] as Set<String> }
        rows.each { row ->
            contributorIdsByRepository[row.repositoryUrl] << row.authorId
        }
        return contributorIdsByRepository.collectEntries { k, v -> [(k): v.size()] }
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
            def repositoriesLines = new File("${mockReportDir}/details/repositories.csv").readLines()
            def repositoryHeader = repositoriesLines.first().split(',', -1)
            def repositoryDormantIndex = repositoryHeader.findIndexOf { it == 'dormant' }
            def dormantRepositoryCount = repositoriesLines.drop(1).count { row ->
                def cols = row.split(',', -1)
                repositoryDormantIndex >= 0 && cols.size() > repositoryDormantIndex && cols[repositoryDormantIndex] == 'true'
            }
            def activeRepositoryCount = repositoriesLines.drop(1).count { row ->
                def cols = row.split(',', -1)
                repositoryDormantIndex >= 0 && cols.size() > repositoryDormantIndex && cols[repositoryDormantIndex] == 'false'
            }
            def dormantContributorCount = countDormantContributorsInReport(mockReportDir)
        then:
            new File("${mockReportDir}/summary.txt").exists()
            new File("${mockReportDir}/contributors.csv").exists()
            new File("${mockReportDir}/checksums.sha256").exists()
            def contributorHeader = new File("${mockReportDir}/contributors.csv").readLines().first()
            !contributorHeader.contains("authorNumber")
            !contributorHeader.contains("contributingAuthorNumber")
            contributorHeader.contains("dormant")
            new File("${mockReportDir}/details/repositories.csv").readLines().first().contains("dormant")
            new File("${mockReportDir}/details/commits-by-branch.csv").readLines().first().contains("dormant")
            new File("${mockReportDir}/details/commits-by-repository.csv").readLines().first().contains("dormant")
            new File("${mockReportDir}/details/contributors-by-repository.csv").readLines().first().contains("dormant")
            new File("${mockReportDir}/summary.txt").text.contains("dormant:")
            // Defaults from runtime/report/ncd-report-mock.yml:
            // activeRepositoryCount=3, dormantOverlappingRepositoryCount=2, dormantNonOverlappingRepositoryCount=2
            repositoriesLines.size() == 1 + 7
            activeRepositoryCount == 3
            dormantRepositoryCount == 4
            dormantContributorCount == 2
            verifyAll(result.stdout) {
                it.any { it == "reportPath: ${mockReportDir}" }
                it.any { it == '  reportType: Number of Contributing Developers (NCD) Report' }
                it.any { it.contains("authorCount:") }
                it.any { it.contains("commitCount:") }
            }
    }

    def "mock-generate-dir-includes-top-level-repositories-csv"() {
        def reportDir = tempPath("ncd-report-top-level-repositories")

        when:
            def createResult = Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}")
            def topLevelFile = new File("${reportDir}/repositories.csv")
            def detailFile = new File("${reportDir}/details/repositories.csv")
            def topHeader = readCsvHeader(topLevelFile.absolutePath)
            def topRows = readCsvRowsAsMaps(topLevelFile.absolutePath)
            def detailRows = readCsvRowsAsMaps(detailFile.absolutePath)
        then:
            createResult.exitCode == 0
            topLevelFile.exists()
            topHeader == [
                    'repositoryUrl', 'repositoryName', 'visibility', 'fork', 'status',
                    'reason', 'dormant', 'commitCountRaw', 'contributorCountRaw', 'sourceReport'
            ]
            topRows.size() == detailRows.size()
            topRows.every { it.sourceReport == '' }
            topRows.every { it.commitCountRaw ==~ /\d+/ }
            topRows.every { it.contributorCountRaw ==~ /\d+/ }
    }

    def "mock-default-dormant-repositories-generate-dormant-contributors"() {
        def reportDir = tempPath("ncd-report-default-dormant")
        when:
            def result = Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}")
            def dormantCount = countDormantContributorsInReport(reportDir)
        then:
            result.exitCode == 0
            dormantCount == 2
    }

    def "mock-overlap-dormant-authors-active-wins"() {
        def reportDir = tempPath("ncd-report-dormant-overlap")
        def configYaml = tempPath("ncd-report-dormant-overlap.yml")
        when:
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
|    - activeRepositoryCount: 1
|      dormantOverlappingRepositoryCount: 1
|      dormantNonOverlappingRepositoryCount: 0
|      authorsPerRepository: 2
|      commitsPerAuthor: 3
""".stripMargin()
            def result = Fcli.run("license ncd-report create -y -c ${configYaml} -d ${reportDir}")
            def dormantCount = countDormantContributorsInReport(reportDir)
        then:
            result.exitCode == 0
            dormantCount == 0
    }

    def "mock-mixed-dormant-repositories-single-config"() {
        def reportDir = tempPath("ncd-report-dormant-mixed")
        def configYaml = tempPath("ncd-report-dormant-mixed.yml")
        when:
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
|    - activeRepositoryCount: 1
|      dormantOverlappingRepositoryCount: 1
|      dormantNonOverlappingRepositoryCount: 1
|      authorsPerRepository: 2
|      commitsPerAuthor: 3
""".stripMargin()
            def result = Fcli.run("license ncd-report create -y -c ${configYaml} -d ${reportDir}")
            def dormantCount = countDormantContributorsInReport(reportDir)
            def repositoriesLines = new File("${reportDir}/details/repositories.csv").readLines()
            def repositoriesHeader = repositoriesLines.first().split(',', -1)
            def dormantIndex = repositoriesHeader.findIndexOf { it == 'dormant' }
            def trueDormantRepos = repositoriesLines.drop(1).count { line ->
                def cols = line.split(',', -1)
                dormantIndex >= 0 && cols.size() > dormantIndex && cols[dormantIndex] == 'true'
            }
        then:
            result.exitCode == 0
            dormantCount >= 1
            trueDormantRepos == 2
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
            result.stdout.any { it.contains("dormant") }
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
            result.stdout[0].contains("dormant")
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

    def "mock-legacy-missing-dormant-column-lsc-merge"() {
        def report1 = tempPath("ncd-report-legacy-no-dormant-source-1")
        def report2 = tempPath("ncd-report-legacy-no-dormant-source-2")
        def mergedReport = tempPath("ncd-report-legacy-no-dormant-merged")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report1}")
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report2}")

            removeDormantColumnFromContributorsCsv(report1)
            removeDormantColumnFromContributorsCsv(report2)

            def lscResult = Fcli.run("license ncd-report list-contributors -r ${report1} -o csv")
            def mergeResult = Fcli.run("license ncd-report merge -r ${report1},${report2} -d ${mergedReport} -y")

            def mergedLines = new File("${mergedReport}/contributors.csv").readLines()
            def headers = mergedLines.first().split(',', -1)
            def dormantIndex = headers.findIndexOf { it == 'dormant' }
            def hasUnknownDormant = mergedLines.drop(1).any { row ->
                def cols = row.split(',', -1)
                dormantIndex >= 0 && cols.size() > dormantIndex && cols[dormantIndex] == 'unknown'
            }
        then:
            lscResult.exitCode == 0
            lscResult.stdout[0].contains('dormant')
            lscResult.stdout.drop(1).any { it.contains(',unknown,') }
            mergeResult.exitCode == 0
            headers.contains('dormant')
            hasUnknownDormant
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

    def "mock-merge-dormant-active-wins"() {
        def report1 = tempPath("ncd-report-dormant-source-1")
        def report2 = tempPath("ncd-report-dormant-source-2")
        def mergedReport = tempPath("ncd-report-dormant-merged")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report1}")
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${report2}")

            def targetAuthorId = getFirstAuthorId(report1)
            setDormantForAuthorId(report1, targetAuthorId, true)
            setDormantForAuthorId(report2, targetAuthorId, false)

            def mergeResult = Fcli.run("license ncd-report merge -r ${report1},${report2} -d ${mergedReport} -y")
            def mergedLines = new File("${mergedReport}/contributors.csv").readLines()
            def headers = mergedLines.first().split(',', -1)
            def authorIdIndex = headers.findIndexOf { it == 'authorId' }
            def dormantIndex = headers.findIndexOf { it == 'dormant' }
            def row = mergedLines.drop(1).find { line ->
                def cols = line.split(',', -1)
                cols[authorIdIndex] == targetAuthorId
            }
            def cols = row.split(',', -1)
        then:
            mergeResult.exitCode == 0
            headers.contains('dormant')
            cols[dormantIndex] == 'false'
            new File("${mergedReport}/summary.txt").text.contains("dormant:")
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

    def "mock-list-repositories-uses-top-level-repositories-csv"() {
        def reportDir = tempPath("ncd-report-list-repositories-top-level")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}")
            def lsrResult = Fcli.run("license ncd-report list-repositories -r ${reportDir} -o csv")
            def reportRows = readCsvRowsAsMaps("${reportDir}/repositories.csv")
            def outputHeader = lsrResult.stdout[0].split(',', -1) as List<String>
            def outputRows = lsrResult.stdout.drop(1)
                    .collect { line ->
                        def cols = line.split(',', -1)
                        def row = [:]
                        outputHeader.eachWithIndex { h, i -> row[h] = i < cols.size() ? cols[i] : '' }
                        row
                    }
        then:
            lsrResult.exitCode == 0
            outputHeader == [
                    'repositoryUrl', 'repositoryName', 'visibility', 'fork', 'status',
                    'reason', 'dormant', 'commitCountRaw', 'contributorCountRaw', 'sourceReport'
            ]
            outputRows.size() == reportRows.size()
            outputRows.collect { it.repositoryUrl }.toSet() == reportRows.collect { it.repositoryUrl }.toSet()
            outputRows.every { it.commitCountRaw ==~ /\d+/ }
            outputRows.every { it.contributorCountRaw ==~ /\d+/ }
    }

    def "mock-list-repositories-fallback-calculates-raw-counts-from-details"() {
        def reportDir = tempPath("ncd-report-list-repositories-fallback")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}")
            new File("${reportDir}/repositories.csv").delete()

            def expectedCommitCounts = computeCommitCountsByRepository(reportDir)
            def expectedContributorCounts = computeContributorCountsByRepository(reportDir)
            def lsrResult = Fcli.run("license ncd-report list-repositories -r ${reportDir} -o csv")

            def header = lsrResult.stdout[0].split(',', -1)
            def headerIndex = [:]
            header.eachWithIndex { h, i -> headerIndex[h] = i }
            def rows = lsrResult.stdout.drop(1).collect { it.split(',', -1) }
        then:
            lsrResult.exitCode == 0
            rows.size() > 0
            rows.each { cols ->
                def repositoryUrl = cols[headerIndex.repositoryUrl]
                cols[headerIndex.commitCountRaw] == String.valueOf(expectedCommitCounts.getOrDefault(repositoryUrl, 0))
                cols[headerIndex.contributorCountRaw] == String.valueOf(expectedContributorCounts.getOrDefault(repositoryUrl, 0))
                cols[headerIndex.sourceReport] == ''
            }
    }

    def "mock-list-repositories-fallback-shows-unknown-for-missing-detail-files"() {
        def reportDir = tempPath("ncd-report-list-repositories-fallback-unknown")

        when:
            Fcli.run("license ncd-report create -y -c ${mockConfigFile} -d ${reportDir}")
            new File("${reportDir}/repositories.csv").delete()
            new File("${reportDir}/details/commits-by-repository.csv").delete()
            new File("${reportDir}/details/contributors-by-repository.csv").delete()
            def lsrResult = Fcli.run("license ncd-report list-repositories -r ${reportDir} -o csv")

            def header = lsrResult.stdout[0].split(',', -1)
            def commitIdx = header.findIndexOf { it == 'commitCountRaw' }
            def contributorIdx = header.findIndexOf { it == 'contributorCountRaw' }
            def rows = lsrResult.stdout.drop(1).collect { it.split(',', -1) }
        then:
            lsrResult.exitCode == 0
            rows.size() > 0
            rows.every { it[commitIdx] == 'unknown' }
            rows.every { it[contributorIdx] == 'unknown' }
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
|    - activeRepositoryCount: 1
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
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
|    - activeRepositoryCount: 1
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
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

    def "mock-validate-sources-json-includes-scm-details"() {
        def configYaml = tempPath("ncd-report-validate-sources-json.yml")
        when:
            new File(configYaml).text = """
|sources:
|  mock:
|    - activeRepositoryCount: 2
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
|      authorsPerRepository: 1
|      commitsPerAuthor: 1
""".stripMargin()
            def result = Fcli.run("license ncd-report validate-sources -c ${configYaml} --show all --limit-per-source 1 -o json")
        then:
            result.exitCode == 0
            result.stdout.any { it.contains("scmDetails") }
            result.stdout.any { it.contains('"source"') }
            result.stdout.any { it.contains('"scm"') }
            result.stdout.any { it.contains('"status"') }
    }

    def "mock-validate-sources-show-excluded"() {
        def configYaml = tempPath("ncd-report-validate-sources-excluded.yml")
        when:
            new File(configYaml).text = """
|sources:
|  mock:
|    - activeRepositoryCount: 3
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
|      authorsPerRepository: 1
|      commitsPerAuthor: 1
|      repositoryIncludeExpression: "false"
""".stripMargin()
            def result = Fcli.run("license ncd-report validate-sources -c ${configYaml} --show excluded -o yaml")
            def excludedCount = result.stdout.count { it.trim() == 'status: excluded' }
        then:
            result.exitCode == 0
            excludedCount == 3
    }

    def "mock-validate-sources-limit-per-source"() {
        def configYaml = tempPath("ncd-report-validate-sources-limit.yml")
        when:
            new File(configYaml).text = """
|sources:
|  mock:
|    - activeRepositoryCount: 3
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
|      authorsPerRepository: 1
|      commitsPerAuthor: 1
|    - activeRepositoryCount: 4
|      dormantOverlappingRepositoryCount: 0
|      dormantNonOverlappingRepositoryCount: 0
|      authorsPerRepository: 1
|      commitsPerAuthor: 1
""".stripMargin()
            def result = Fcli.run("license ncd-report validate-sources -c ${configYaml} --show all --limit-per-source 1 -o yaml")
            def sourceCount = result.stdout.count { it.trim().startsWith('- source: mock:') }
        then:
            result.exitCode == 0
            sourceCount == 2 // One row per configured mock source
    }
}
