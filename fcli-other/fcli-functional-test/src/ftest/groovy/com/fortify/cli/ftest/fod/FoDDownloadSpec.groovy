package com.fortify.cli.ftest.fod;

import static com.fortify.cli.ftest._common.spec.FcliSession.FcliSessionType.FOD

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Requires
import spock.lang.Shared

// Downloads from an existing release (ft.fod.release, with at least one completed static scan and a
// downloadable release-level FPR) and an existing report (ft.fod.report-id), as creating these as part
// of the tests would take too long
@Prefix("fod.download") @FcliSession(FOD)
class FoDDownloadSpec extends FcliBaseSpec {
    @Shared @TempDir("fod/download") String tempDir;

    @Requires({System.getProperty('ft.fod.release')})
    def "sast-scan.download-latest"() {
        def file = new File(tempDir, "latest.fpr")
        when:
            def result = Fcli.run(["fod", "sast-scan", "download-latest", "--rel="+System.getProperty("ft.fod.release"), "-f", file.absolutePath])
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("SCAN_DOWNLOADED") }
            }
            isZip(file)
    }

    @Requires({System.getProperty('ft.fod.release')})
    def "sast-scan.download"() {
        def file = new File(tempDir, "byId.fpr")
        when:
            Fcli.run(["fod", "sast-scan", "list", "--rel="+System.getProperty("ft.fod.release"),
                "-q", "analysisStatusType=='Completed'", "--store", "fodDownloadScans"])
            def result = Fcli.run(["fod", "sast-scan", "download", "::fodDownloadScans::get(0).scanId", "-f", file.absolutePath])
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("SCAN_DOWNLOADED") }
            }
            isZip(file)
    }

    @Requires({System.getProperty('ft.fod.report-id')})
    def "report.download"() {
        def file = new File(tempDir, "report.zip")
        when:
            def result = Fcli.run(["fod", "report", "download", System.getProperty("ft.fod.report-id"), "-f", file.absolutePath])
        then:
            verifyAll(result.stdout) {
                it.any { it.contains("REPORT_DOWNLOADED") }
            }
            file.length() > 0
    }

    private static boolean isZip(File file) {
        file.length() > 0 && file.withInputStream { is -> is.readNBytes(2) } == "PK".bytes
    }
}
