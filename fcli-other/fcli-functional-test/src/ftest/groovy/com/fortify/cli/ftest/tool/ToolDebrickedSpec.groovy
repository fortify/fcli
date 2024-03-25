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

@Prefix("tool.debricked") @Stepwise
class ToolDebrickedSpec extends FcliBaseSpec {
    @Shared @TempDir("fortify/tools") String baseDir;
    @Shared String version = "1.7.12"
    @Shared Path globalBinScript = Path.of(baseDir).resolve("bin/debricked.bat");
    @Shared Path binScript = Path.of(baseDir).resolve("debricked-cli/${version}/bin/debricked.exe");
    
    def "install"() {
        def args = "tool debricked-cli install -y -v=${version} -b ${baseDir} --platform windows/x64"
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
        def args = "tool debricked-cli list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldir")
                it[1].replace(" ", "").startsWith("debricked")
            }
    }
    
    def "uninstall"() {
        def args = "tool debricked-cli uninstall -y -v=${version}"
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
    
}
