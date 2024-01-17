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

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier.SSCRole
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

@Prefix("tool.vuln-exporter") @Stepwise
class ToolVulnExporterSpec extends FcliBaseSpec {
    
    def "install"() {
        def args = "tool vuln-exporter install -y -v=latest"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasforStableInstalldirAction")
                it[1].contains(" INSTALLED")
            }
    }
    
    def "listVersions"() {
        def args = "tool vuln-exporter list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasforStableInstalldir")
                it[1].replace(" ", "").startsWith("vuln-exporter")
            }
    }
    
    def "uninstall"() {
        def args = "tool vuln-exporter uninstall -y -v=default"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasforStableInstalldirAction")
                it[1].contains(" UNINSTALLED")
            }
    }
    
}
