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

@Prefix("tool.definitions.update") @Stepwise
class ToolDefinitionsSpec extends FcliBaseSpec {
    
    def "updateDefault"() {
        def args = "tool definitions update"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("SourceLastupdateAction")
                it[1].replace(" ", "").contains("https://github.com/")
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
                it[0].replace(' ', '').equals("NameVersionAliasesStableInstalldir")
                it[1].replace(" ", "").startsWith("debricked")
            }
    }
    
    /*
    // TODO The tool definitions hosted on this URL are no longer valid due to yaml structure changes
    def "updateWithUrl"() {
        def args = "tool definitions update --source https://github.com/psmf22/tool-definitions/raw/main/v1/tool-definitions.yaml.zip"
        when:
            def result = Fcli.run(args)
        then:
            verifyAll(result.stdout) {
                size()>0
                it[0].replace(' ', '').equals("SourceLastupdateAction")
                it[1].replace(" ", "").contains("https://github.com/psmf22/tool-definitions/raw/main/v1/tool-definitions")
                it[1].contains(" UPDATED")
            }
    }
    */
    
    def "listVersions2"() {
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
