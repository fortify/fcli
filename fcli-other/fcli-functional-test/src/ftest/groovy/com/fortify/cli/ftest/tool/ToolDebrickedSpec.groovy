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

@Prefix("tool.debricked") @Stepwise
class ToolDebrickedSpec extends FcliBaseSpec {

    def "install"() {
        def args = "tool debricked install -y -v=latest"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionDefaultInstalledInstalldirBindirOperatingsystemCpuarchitectureAction")
                it[1].replace(" ", "").contains("YesYes") || it[1].replace(" ", "").contains("YesYes")
                it[1].contains("INSTALLED")
            }
    }
    
    def "listVersions"() {
        def args = "tool debricked list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionDefaultInstalledInstalldirBindirOperatingsystemCpuarchitecture")
                it[it.size()-2].replace(" ", "").startsWith("debricked")
                it[it.size()-2].replace(" ", "").contains("YesYes") || it[it.size()-2].replace(" ", "").contains("NoYes")
            }
    }
    
    def "uninstall"() {
        def args = "tool debricked uninstall -y -v=default -a=x86_64"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionDefaultInstalledInstalldirBindirOperatingsystemCpuarchitectureAction")
                it[1].replace(" ", "").contains("UNINSTALLED")
            }
    }
    
}
