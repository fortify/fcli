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

@Prefix("tool.config.update") @Stepwise
class ToolConfigUpdateSpec extends FcliBaseSpec {
    
    def "updateDefault"() {
        def args = "tool config update"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("RemotepathLocalpathAction")
                it[1].replace(" ", "").contains("https://github.com/fortify-ps/tool-definitions/raw/main/v1/tool-definitions")
                it[1].contains(" UPDATED")
            }
    }
    
    def "listVersions"() {
        def args = "tool debricked-cli list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasforStableInstalldir")
                it[1].replace(" ", "").startsWith("debricked")
            }
    }
    
    def "updateWithUrl"() {
        def args = "tool config update --url https://github.com/psmf22/tool-definitions/raw/main/v1/tool-definitions.yaml.zip"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("RemotepathLocalpathAction")
                it[1].replace(" ", "").contains("https://github.com/psmf22/tool-definitions/raw/main/v1/tool-definitions")
                it[1].contains(" UPDATED")
            }
    }
    
    def "listVersions2"() {
        def args = "tool debricked-cli list"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("NameVersionAliasforStableInstalldir")
                it[1].replace(" ", "").startsWith("debricked")
            }
    }
    /*
    def "updateWithLocalPath"() {
        def args = "tool config update --file C:/temp/tool-definitions.yaml.zip"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("RemotepathLocalpathAction")
                it[1].replace(" ", "").contains("C:/temp/tool-definitions.yaml.zip")
                it[1].contains("UPDATED")
            }
    }
    
    def "updateWithUNCPath"() {
        def args = "tool config update --file \\\\localhost\\C\$\\temp\\tool-definitions.yaml.zip"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("RemotepathLocalpathAction")
                it[1].replace(" ", "").contains("\\\\localhost\\C\$\\temp\\tool-definitions.yaml.zip")
                it[1].contains("UPDATED")
            }
    }*/
    
}
