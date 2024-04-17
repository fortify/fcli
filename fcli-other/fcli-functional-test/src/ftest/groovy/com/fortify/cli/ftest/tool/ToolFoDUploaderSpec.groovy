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

@Prefix("tool.fod-uploader") @Stepwise
class ToolFoDUploaderSpec extends FcliBaseSpec {
    @Shared @TempDir("fortify/tools") String baseDir;
    @Shared String version = "5.4.0"
    @Shared Path globalBinScript = Path.of(baseDir).resolve("bin/FoDUpload.bat");
    @Shared Path binScript = Path.of(baseDir).resolve("fod-uploader/${version}/bin/FoDUpload.bat");
    
    def "installLatest"() {
        def args = "tool fod-uploader install -y -v=${version} -b ${baseDir}"
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
        def args = "tool fod-uploader list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldir")
                it[1].replace(" ", "").startsWith("fod-uploader")
            }
    }
    
    def "uninstall"() {
        def args = "tool fod-uploader uninstall -y -v=${version}"
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
    
    def "installV5"() {
        def args = "tool fod-uploader install -y -v=5 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it.any { 
                    it.contains("5.4.0")
                    it.contains(" INSTALLED")
                }
            }
    }
    
    def "installV50"() {
        def args = "tool fod-uploader install -y -v=5.0 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("5.0.1")
                it[1].contains(" INSTALLED")
            }
    }
    
    def "installV500"() {
        def args = "tool fod-uploader install -y -v=5.0.0 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("5.0.0")
                it[1].contains(" INSTALLED")
            }
    }
    
    def "installWithVPrefix"() {
        def args = "tool fod-uploader install -y -v=v5.0.0 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("5.0.0")
                it[1].contains(" INSTALLED")
            }
    }
    
    def "installAndUninstallWithVPrefix"() {
        def args = "tool fod-uploader install -y -v=v5.0.1 --uninstall=v5.0.0 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains("5.0.1")
                it[1].contains(" INSTALLED")
            }
    }
    
    def "uninstallWithVPrefix"() {
        def args = "tool fod-uploader uninstall -y -v=v5.0.1 --progress=none"
        when:
            def result = Fcli.run(args, {it.expectZeroExitCode()})
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldirAction")
                it[1].contains(" UNINSTALLED")
            }
    }
    
}
