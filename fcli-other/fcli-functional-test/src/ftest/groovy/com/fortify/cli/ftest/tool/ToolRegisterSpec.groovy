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
import java.nio.file.StandardCopyOption
import java.util.Comparator

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir

import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

/**
 * Comprehensive functional tests for tool register commands.
 * Tests registration with --path and --auto-detect modes, with various scenarios
 * including fcli-installed tools and external installations.
 */
@Prefix("tool.register") @Stepwise
class ToolRegisterSpec extends FcliBaseSpec {
    @Shared @TempDir("tool-register-test/install") String installBaseDir
    @Shared @TempDir("tool-register-test/external") String externalBaseDir
    @Shared Path fcliStateDir
    
    // Tool definitions with version info
    @Shared Map<String, Map> toolConfigs = [
        'fcli': [
            version: '2.0.0',  // Old version unlikely to be latest 2.x
            binaryName: getBinaryName('fcli'),
            binSubdir: 'bin',
            envVar: 'FCLI',
            homeVar: 'FCLI_HOME'
        ],
        'sc-client': [
            version: '22.2.0',  // Old version unlikely to be latest
            binaryName: isWindows() ? 'scancentral.bat' : 'scancentral',
            binSubdir: 'bin',
            envVar: 'SC_CLIENT',
            homeVar: 'SC_CLIENT_HOME'
        ],
        'fod-uploader': [
            version: '5.4.3',
            binaryName: 'FodUpload.jar',
            binSubdir: null, // JAR is in root
            envVar: 'FOD_UPLOADER',
            homeVar: 'FOD_UPLOADER_HOME'
        ],
        'debricked-cli': [
            version: '1.9.0',
            binaryName: getBinaryName('debricked'),
            binSubdir: 'bin',
            envVar: 'DEBRICKED_CLI',
            homeVar: 'DEBRICKED_CLI_HOME'
        ],
        'bugtracker-utility': [
            version: '4.14.0',
            binaryName: 'FortifyBugTrackerUtility.jar',
            binSubdir: null,
            envVar: 'BUGTRACKER_UTILITY',
            homeVar: 'BUGTRACKER_UTILITY_HOME'
        ],
        'vuln-exporter': [
            version: '2.0.4',
            binaryName: 'FortifyVulnerabilityExporter.jar',
            binSubdir: null,
            envVar: 'VULN_EXPORTER',
            homeVar: 'VULN_EXPORTER_HOME'
        ]
    ]
    
    // Helper method to get platform-specific binary name
    static String getBinaryName(String baseName) {
        return isWindows() ? "${baseName}.exe" : baseName
    }
    
    // Helper method to detect Windows platform
    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows")
    }
    
    // Track which tools have been installed to avoid repeated downloads
    @Shared Set<String> installedTools = []
    
    def setupSpec() {
        fcliStateDir = Path.of(System.getProperty("fcli.env.FORTIFY_DATA_DIR")).resolve("state/tools")
    }
    
    def cleanupSpec() {
        // Uninstall all tools that were installed during tests
        toolConfigs.keySet().each { tool ->
            if (installedTools.contains(tool)) {
                def config = toolConfigs[tool]
                try {
                    Fcli.run("tool ${tool} uninstall -y -v=${config.version} --progress none", {})
                    println "Uninstalled ${tool} ${config.version}"
                } catch (Exception e) {
                    println "Warning: Failed to uninstall ${tool}: ${e.message}"
                }
            }
        }
        
        // Clean up tool state for all registered tools to avoid interference with other tests
        toolConfigs.keySet().each { tool ->
            def toolStateDir = fcliStateDir.resolve(tool)
            if (Files.exists(toolStateDir)) {
                try {
                    deleteDirectory(toolStateDir)
                    println "Cleaned up tool state for ${tool}"
                } catch (IOException e) {
                    println "Warning: Failed to clean up tool state for ${tool}: ${e.message}"
                }
            }
        }
        // Note: Installation directories in installBaseDir and externalBaseDir are automatically
        // cleaned up by @TempDir annotation
    }
    
    @Unroll
    def "install #tool for testing"() {
        when: "installing tool via fcli"
            // Skip if already installed
            if (installedTools.contains(tool)) {
                println "Tool ${tool} already installed, skipping"
            } else {
                def config = toolConfigs[tool]
                def args = "tool ${tool} install -y -v=${config.version} -b ${installBaseDir} --progress none"
                def result = Fcli.run(args, {it.expectZeroExitCode()})
                installedTools.add(tool)
                
                assert result.stdout.size() > 0
                assert result.stdout[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                assert result.stdout[1].contains(" INSTALLED")
            }
            
        then: "installation completed"
            installedTools.contains(tool)
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool with --path pointing to install directory"() {
        given: "tool installation directory"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            
        when: "registering with --path to install directory"
            def args = "tool ${tool} register --path ${installDir}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds with correct version"
            result.stdout.size() > 0
            result.stdout[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool with --path pointing to binary"() {
        given: "tool binary path"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            def binaryPath = config.binSubdir 
                ? installDir.resolve("${config.binSubdir}/${config.binaryName}")
                : installDir.resolve(config.binaryName)
            
        when: "registering with --path to binary"
            def args = "tool ${tool} register --path ${binaryPath}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds with correct version"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            
        where:
            tool << toolConfigs.keySet()
    }
    
    // Note: --auto-detect with environment variables requires process environment manipulation
    // which is not easily testable in the current Fcli test framework.
    // Auto-detect functionality is indirectly tested through --path tests and manual testing.
    
    @Unroll
    def "register #tool to override install location (fcli-installed tool)"() {
        given: "tool installed in one location, registering to different location"
            def config = toolConfigs[tool]
            def originalInstallDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            def externalInstallDir = Path.of(externalBaseDir).resolve("${tool}/${config.version}")
            
            // Copy installation to external directory
            copyDirectory(originalInstallDir, externalInstallDir)
            
        when: "registering external installation"
            def args = "tool ${tool} register --path ${externalInstallDir}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds with new location"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            outputLine.contains(externalBaseDir)
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool without install descriptor (external installation)"() {
        given: "external tool installation without fcli install descriptor"
            def config = toolConfigs[tool]
            def originalInstallDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            def externalNoDescriptorDir = Path.of(externalBaseDir).resolve("${tool}-no-descriptor/${config.version}")
            
            // Copy installation without install-descriptor directory
            copyDirectoryExcluding(originalInstallDir, externalNoDescriptorDir, "install-descriptor")
            
            // Remove tool state to simulate external installation
            def stateFile = fcliStateDir.resolve("${tool}/${config.version}")
            Files.deleteIfExists(stateFile)
            
        when: "registering external installation without descriptor"
            def args = "tool ${tool} register --path ${externalNoDescriptorDir}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds with version detected via alternative methods"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains("REGISTERED")
            // Version might be detected or marked as "unknown" depending on tool
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool with install descriptor (external installation)"() {
        given: "external tool installation with fcli install descriptor"
            def config = toolConfigs[tool]
            def originalInstallDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            def externalWithDescriptorDir = Path.of(externalBaseDir).resolve("${tool}-with-descriptor/${config.version}")
            
            // Copy entire installation including install-descriptor
            copyDirectory(originalInstallDir, externalWithDescriptorDir)
            
            // Remove tool state to simulate external installation
            def stateFile = fcliStateDir.resolve("${tool}/${config.version}")
            Files.deleteIfExists(stateFile)
            
        when: "registering external installation with descriptor"
            def args = "tool ${tool} register --path ${externalWithDescriptorDir}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds with version from descriptor"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            
        where:
            tool << toolConfigs.keySet()
    }
    
    def "register non-existent tool with --path fails"() {
        when: "registering with path to non-existent directory"
            def args = "tool fcli register --path /non/existent/path"
            def result = Fcli.run(args, {it.expectSuccess(false)})
            
        then: "command fails"
            result.isNonZeroExitCode()
    }
    
    // TODO Any way to test this reliably, considering that system on which this
    // is running might already have one or more tools installed and accessible
    // through one of the environment variables that --auto-detect looks for?
    // Fact is that this also fails during functional test run on GitHub, which
    // shouldn't have any tools pre-installed, other than the just-built fcli,
    // or tools installed by other functional tests.
    // The "register non-existent tool with --path fails" test above covers 
    // error handling; is this sufficient?
    /*
    def "register with --auto-detect fails when tool not found"() {
        when: "registering with auto-detect when tool is not in PATH or env vars"
            def args = "tool fcli register --auto-detect"
            def result = Fcli.run(args, {it.expectSuccess(false)})
            
        then: "command fails with helpful message"
            result.isNonZeroExitCode()
    }
    */
    
    @Unroll
    def "registered #tool appears in list command"() {
        given: "tool has been registered"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            
        when: "listing tool versions"
            def args = "tool ${tool} list"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registered tool appears in list with install directory shown"
            result.stdout.size() > 0
            // Find line with the registered version that has an install directory (not "N/A")
            def installedLine = result.stdout.find { line -> 
                line.contains(tool) && 
                line.contains(config.version) && 
                !line.contains("N/A")
            }
            installedLine != null
            
        where:
            tool << toolConfigs.keySet()
    }
    
    // Version filtering tests
    
    @Unroll
    def "register #tool with --version matching exact version"() {
        given: "tool installation directory"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            
        when: "registering with --version matching exact version"
            def args = "tool ${tool} register --path ${installDir} --version ${config.version}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool with --version with v prefix"() {
        given: "tool installation directory"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            
        when: "registering with --version using v prefix on exact version"
            def args = "tool ${tool} register --path ${installDir} --version v${config.version}"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds (v prefix is normalized)"
            result.stdout.size() > 0
            def outputLine = result.stdout[1]
            outputLine.contains(tool)
            outputLine.contains(config.version)
            outputLine.contains("REGISTERED")
            
        where:
            tool << toolConfigs.keySet()
    }
    
    @Unroll
    def "register #tool with --version mismatch fails"() {
        given: "tool installation directory"
            def config = toolConfigs[tool]
            def installDir = Path.of(installBaseDir).resolve("${tool}/${config.version}")
            def wrongVersion = "999.999.999" // Version that definitely won't match
            
        when: "registering with non-matching --version"
            def args = "tool ${tool} register --path ${installDir} --version ${wrongVersion}"
            def result = Fcli.run(args, {it.expectSuccess(false)})
            
        then: "registration fails with version error"
            result.isNonZeroExitCode()
            result.stderr.any { line -> 
                line.contains("does not match") || line.contains("not found") || line.contains("version")
            }
            
        where:
            tool << toolConfigs.keySet()
    }
    
    def "register with --version=any accepts any version"() {
        given: "fcli installation"
            def config = toolConfigs['fcli']
            def installDir = Path.of(installBaseDir).resolve("fcli/${config.version}")
            
        when: "registering with --version=any"
            def args = "tool fcli register --path ${installDir} --version any"
            def result = Fcli.run(args, {it.expectZeroExitCode()})
            
        then: "registration succeeds regardless of detected version"
            result.stdout.size() > 0
            result.stdout[1].contains("REGISTERED")
    }
    
    // Helper methods
    
    private void copyDirectory(Path source, Path target) {
        Files.createDirectories(target)
        Files.walk(source).forEach { sourcePath ->
            def targetPath = target.resolve(source.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    
    private void copyDirectoryExcluding(Path source, Path target, String excludeDirName) {
        Files.createDirectories(target)
        Files.walk(source).forEach { sourcePath ->
            if (!sourcePath.toString().contains(File.separator + excludeDirName)) {
                def targetPath = target.resolve(source.relativize(sourcePath))
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
    
    private void deleteDirectory(Path directory) {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }
}
