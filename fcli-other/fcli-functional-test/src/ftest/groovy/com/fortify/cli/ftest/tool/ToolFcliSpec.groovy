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
package com.fortify.cli.ftest.tool

import java.nio.file.Files
import java.nio.file.Path

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("tool.fcli") @Stepwise
class ToolFcliSpec extends FcliBaseSpec {
    @Shared @TempDir("fortify/tools") String baseDir;
    @Shared String version = "2.1.0"
    @Shared String platform = System.getProperty("os.name").toLowerCase().contains("windows") ? "windows/x64" : "linux/x64"
    @Shared String binaryExt = System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""
    @Shared String scriptExt = System.getProperty("os.name").toLowerCase().contains("windows") ? ".bat" : ""
    @Shared Path globalBinScript = Path.of(baseDir).resolve("bin/fcli${scriptExt}");
    @Shared Path binScript = Path.of(baseDir).resolve("fcli/${version}/bin/fcli${binaryExt}");
    
    def "install"() {
        def args = "tool fcli install -y -v=${version} -b ${baseDir} --platform ${platform} --progress none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains(" INSTALLED")
                Files.exists(binScript);
                Files.exists(globalBinScript);
            }
    }
    
    def "listVersions"() {
        def args = "tool fcli list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldir")
                it[1].replace(" ", "").startsWith("fcli")
            }
    }
    
    def "uninstall"() {
        def args = "tool fcli uninstall -y -v=${version} --progress none"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains(" UNINSTALLED")
                // TODO fcli currently doesn't delete/update global bin script
                Files.exists(globalBinScript);
                !Files.exists(binScript);
            }
    }
    
    def "installWithCopyFrom"() {
        def copyFromVersion = "2.2.0"
        def sourceInstallDir = Path.of(baseDir).resolve("fcli/${copyFromVersion}")
        def sourceBinScript = sourceInstallDir.resolve("bin/fcli${binaryExt}")
        def globalBinScript = Path.of(baseDir).resolve("bin/fcli${scriptExt}")
        
        // First install the source version to copy from
        def installArgs = "tool fcli install -y -v=${copyFromVersion} -b ${baseDir} --platform ${platform} --progress none"
        Fcli.run(installArgs, {it.expectZeroExitCode()})
        
        // Verify source installation exists
        assert Files.exists(sourceBinScript)
        
        // Now reinstall using --copy-if-matching (should detect version and skip if already installed)
        def copyArgs = "tool fcli install -y --copy-if-matching ${sourceBinScript} -b ${baseDir} --progress none"
        
        when:
            def result = Fcli.run(copyArgs, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("fcli")
                it[1].contains(copyFromVersion)
                // Accept COPIED (successful copy) or SKIPPED_EXISTING (already installed)
                it[1].contains("COPIED") || it[1].contains("SKIPPED_EXISTING")
                Files.exists(sourceBinScript)
                Files.exists(globalBinScript)
            }
        
        cleanup:
            // Uninstall the version
            Fcli.run("tool fcli uninstall -y -v=${copyFromVersion} --progress none")
    }
    
    def "installWithCopyFromBinDir"() {
        def sourceVersion = "2.3.0"
        def sourceBinDir = Path.of(baseDir).resolve("fcli/${sourceVersion}/bin")
        def sourceBinScript = sourceBinDir.resolve("fcli${binaryExt}")
        
        // First install the source version
        def installArgs = "tool fcli install -y -v=${sourceVersion} -b ${baseDir} --platform ${platform} --progress none"
        Fcli.run(installArgs, {it.expectZeroExitCode()})
        
        // Now use --copy-if-matching with the bin directory
        def copyArgs = "tool fcli install -y --copy-if-matching ${sourceBinDir} -b ${baseDir} --progress none"
        
        when:
            def result = Fcli.run(copyArgs, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("fcli")
                it[1].contains(sourceVersion)
                // Accept COPIED (successful copy) or SKIPPED_EXISTING (already installed)
                it[1].contains("COPIED") || it[1].contains("SKIPPED_EXISTING")
                Files.exists(sourceBinScript)
            }
        
        cleanup:
            // Uninstall the version
            Fcli.run("tool fcli uninstall -y -v=${sourceVersion} --progress none")
    }
    
    def "installWithCopyFromInstallDir"() {
        def sourceVersion = "2.4.0"
        def sourceInstallDir = Path.of(baseDir).resolve("fcli/${sourceVersion}")
        def sourceBinScript = sourceInstallDir.resolve("bin/fcli${binaryExt}")
        
        // First install the source version
        def installArgs = "tool fcli install -y -v=${sourceVersion} -b ${baseDir} --platform ${platform} --progress none"
        Fcli.run(installArgs, {it.expectZeroExitCode()})
        
        // Now use --copy-if-matching with the install directory
        def copyArgs = "tool fcli install -y --copy-if-matching ${sourceInstallDir} -b ${baseDir} --progress none"
        
        when:
            def result = Fcli.run(copyArgs, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("fcli")
                it[1].contains(sourceVersion)
                // Accept COPIED (successful copy) or SKIPPED_EXISTING (already installed)
                it[1].contains("COPIED") || it[1].contains("SKIPPED_EXISTING")
                Files.exists(sourceBinScript)
            }
        
        cleanup:
            // Uninstall the version
            Fcli.run("tool fcli uninstall -y -v=${sourceVersion} --progress none")
    }
    
    def "installWithCopyFromNonExecutable"() {
        def nonExecutablePath = Path.of(baseDir).resolve("non-executable.txt")
        Files.createDirectories(nonExecutablePath.getParent())
        Files.writeString(nonExecutablePath, "not an executable")
        
        def copyArgs = "tool fcli install -y --copy-if-matching ${nonExecutablePath} -b ${baseDir} --progress none"
        
        when:
            def result = Fcli.run(copyArgs, {it.expectZeroExitCode()})
        then:
            // When copy-if-matching source is invalid, it should fall back to download
            // and successfully install the latest version
            result.exitCode == 0
            result.stdout.size() > 0
            result.stdout.any { it.contains("fcli") && (it.contains(" INSTALLED") || it.contains(" SKIPPED_EXISTING")) }
        
        cleanup:
            Files.deleteIfExists(nonExecutablePath)
    }
    
    def cleanupSpec() {
        cleanupAllTools()
    }
}
